package com.fotmob.translator.ui.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.fotmob.translator.FotMobApplication
import com.fotmob.translator.R
import com.fotmob.translator.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class NewsDetailFragment : Fragment() {

    private lateinit var app: FotMobApplication

    private var progressBar: ProgressBar? = null
    private var tvTitle: TextView? = null
    private var tvContent: TextView? = null
    private var tvDate: TextView? = null
    private var tvSource: TextView? = null
    private var tvTranslating: TextView? = null
    private var ivThumbnail: ImageView? = null
    private var btnOpenBrowser: Button? = null

    private var newsUrl: String = ""
    private var newsTitle: String = ""
    private var newsSummary: String = ""
    private var thumbnailUrl: String? = null
    private var publishedTime: Long = 0
    private var source: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            newsUrl = it.getString("url", "")
            newsTitle = it.getString("title", "")
            newsSummary = it.getString("summary", "")
            thumbnailUrl = it.getString("thumbnailUrl")
            publishedTime = it.getLong("publishedTime", 0L)
            source = it.getString("source")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_news_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FotMobApplication

        progressBar = view.findViewById(R.id.progress_bar)
        tvTitle = view.findViewById(R.id.tv_detail_title)
        tvContent = view.findViewById(R.id.tv_detail_content)
        tvDate = view.findViewById(R.id.tv_detail_date)
        tvSource = view.findViewById(R.id.tv_detail_source)
        tvTranslating = view.findViewById(R.id.tv_translating)
        ivThumbnail = view.findViewById(R.id.iv_detail_thumbnail)
        btnOpenBrowser = view.findViewById(R.id.btn_open_browser)

        // 显示缩略图
        thumbnailUrl?.let { url ->
            if (url.isNotBlank()) {
                ivThumbnail?.visibility = View.VISIBLE
                ivThumbnail?.load(url) { crossfade(true) }
            }
        }

        // 显示来源和时间
        tvDate?.text = DateUtils.formatFullDate(publishedTime)
        source?.let {
            tvSource?.text = it
            tvSource?.visibility = View.VISIBLE
        }

        // 在浏览器中打开按钮
        btnOpenBrowser?.setOnClickListener {
            if (newsUrl.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(newsUrl)))
            }
        }

        // 先显示标题和摘要，然后加载并翻译正文
        tvTitle?.text = newsTitle
        if (newsSummary.isNotBlank()) {
            tvContent?.text = newsSummary
        }

        // 加载并翻译
        loadAndTranslateContent()
    }

    private fun loadAndTranslateContent() {
        if (newsUrl.isBlank()) {
            tvContent?.text = newsSummary
            return
        }

        progressBar?.visibility = View.VISIBLE
        tvTranslating?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Step 1: 抓取网页正文
                val articleContent = withContext(Dispatchers.IO) {
                    fetchArticleContent(newsUrl)
                }

                if (articleContent.isBlank()) {
                    tvTranslating?.text = "无法获取文章内容"
                    tvContent?.text = newsSummary
                    progressBar?.visibility = View.GONE
                    return@launch
                }

                // Step 2: 翻译标题
                val translatedTitle = withContext(Dispatchers.IO) {
                    app.translator.translate(newsTitle)
                }
                tvTitle?.text = translatedTitle

                // Step 3: 翻译正文（分段翻译）
                tvTranslating?.text = "正在翻译内容..."
                val translatedContent = withContext(Dispatchers.IO) {
                    translateLongText(articleContent)
                }

                tvContent?.text = translatedContent
                tvTranslating?.visibility = View.GONE
            } catch (e: Exception) {
                tvTranslating?.text = "翻译失败，显示原文"
                tvContent?.text = newsSummary.ifBlank { "无法加载内容" }
            } finally {
                progressBar?.visibility = View.GONE
            }
        }
    }

    private fun fetchArticleContent(url: String): String {
        return try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 16; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return ""

            val doc: Document = Jsoup.parse(html)

            // 移除不需要的元素
            doc.select("script, style, nav, header, footer, .ad, .advertisement, .sidebar, .comments, iframe, noscript").remove()

            // 尝试多种文章正文选择器
            val article = doc.selectFirst("article")
                ?: doc.selectFirst("[class*='article-body']")
                ?: doc.selectFirst("[class*='article_content']")
                ?: doc.selectFirst("[class*='post-content']")
                ?: doc.selectFirst("[class*='entry-content']")
                ?: doc.selectFirst("[class*='story-body']")
                ?: doc.selectFirst("main")
                ?: doc.selectFirst("[role='main']")

            val content = article?.text()?.trim() ?: ""

            // 如果内容太短，尝试获取所有段落
            if (content.length < 100) {
                val paragraphs = doc.select("p")
                val fullText = paragraphs.joinToString("\n\n") { it.text().trim() }
                    .replace(Regex("\\n{3,}"), "\n\n")
                    .trim()
                return if (fullText.length > content.length) fullText else content
            }

            content
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 将长文本分段翻译，每段不超过 4500 字符
     */
    private suspend fun translateLongText(text: String): String {
        // 按段落分割
        val paragraphs = text.split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.length > 5 } // 过滤太短的段落

        if (paragraphs.isEmpty()) return text

        val translatedParagraphs = mutableListOf<String>()
        val batch = mutableListOf<String>()
        var currentBatchLength = 0
        val MAX_BATCH_CHARS = 4500

        for (paragraph in paragraphs) {
            if (currentBatchLength + paragraph.length > MAX_BATCH_CHARS && batch.isNotEmpty()) {
                // 翻译当前批次
                val translated = app.translator.translateBatch(batch)
                translatedParagraphs.addAll(translated)
                batch.clear()
                currentBatchLength = 0
            }
            batch.add(paragraph)
            currentBatchLength += paragraph.length
        }

        // 翻译剩余的
        if (batch.isNotEmpty()) {
            val translated = app.translator.translateBatch(batch)
            translatedParagraphs.addAll(translated)
        }

        return translatedParagraphs.joinToString("\n\n")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressBar = null
        tvTitle = null
        tvContent = null
        tvDate = null
        tvSource = null
        tvTranslating = null
        ivThumbnail = null
        btnOpenBrowser = null
    }
}
