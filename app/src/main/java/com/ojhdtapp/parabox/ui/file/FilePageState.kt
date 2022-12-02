package com.ojhdtapp.parabox.ui.file

import android.app.Application
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.toFormattedDate
import com.ojhdtapp.parabox.domain.model.File
import kotlin.math.abs

data class FilePageState(
    val area: Int = FilePageState.MAIN_AREA,
    val isLoading: Boolean = true,
    val data: List<File> = emptyList(),
    val timeFilter: TimeFilter = TimeFilter.All,
    val extensionFilter: ExtensionFilter = ExtensionFilter.All,
    val sizeFilter: SizeFilter = SizeFilter.All,
    val enableRecentDocsFilter: Boolean = false,
    val enableRecentSlidesFilter: Boolean = false,
    val enableRecentSheetsFilter: Boolean = false,
    val enableRecentPictureFilter: Boolean = false,
    val enableRecentAudioFilter: Boolean = false,
    val enableRecentVideoFilter: Boolean = false,
    val enableRecentCompressedFilter: Boolean = false,
    val enableRecentPDFFilter: Boolean = false,
) {
    companion object {
        const val MAIN_AREA = 0
        const val SEARCH_AREA = 1
    }

    val filterData
        get() = data
            .filter {
                timeFilter.fileCheck(it)
                        && extensionFilter.fileCheck(it)
                        && sizeFilter.fileCheck(it)
            }
    val recentFilterData
        get() = data.filter {
            if (listOf<Boolean>(
                    enableRecentDocsFilter,
                    enableRecentSlidesFilter,
                    enableRecentSheetsFilter,
                    enableRecentPictureFilter,
                    enableRecentAudioFilter,
                    enableRecentVideoFilter,
                    enableRecentCompressedFilter,
                    enableRecentPDFFilter
                ).all { !it }
            ) true
            else {
                (if (enableRecentDocsFilter) ExtensionFilter.Docs.fileCheck(it) else false)
                        || (if (enableRecentSlidesFilter) ExtensionFilter.Slides.fileCheck(it) else false)
                        || (if (enableRecentSheetsFilter) ExtensionFilter.Sheets.fileCheck(it) else false)
                        || (if (enableRecentPictureFilter) ExtensionFilter.Picture.fileCheck(it) else false)
                        || (if (enableRecentAudioFilter) ExtensionFilter.Audio.fileCheck(it) else false)
                        || (if (enableRecentVideoFilter) ExtensionFilter.Video.fileCheck(it) else false)
                        || (if (enableRecentCompressedFilter) ExtensionFilter.Compressed.fileCheck(
                    it
                ) else false)
                        || (if (enableRecentPDFFilter) ExtensionFilter.Pdf.fileCheck(it) else false)
            }
        }
}

sealed class TimeFilter(
    val labelResId: Int,
    val fileCheck: (file: File) -> Boolean
) {
    object All : TimeFilter(R.string.time_filter_all_label, { true })
    object WithinThreeDays : TimeFilter(
        R.string.time_filter_within_three_days_label,
        { file: File -> abs(System.currentTimeMillis() - file.timestamp) < 259200000 })

    object WithinThisWeek : TimeFilter(
        R.string.time_filter_within_this_week_label,
        { file: File -> abs(System.currentTimeMillis() - file.timestamp) < 604800000 }
    )

    object WithinThisMonth : TimeFilter(
        R.string.time_filter_within_this_month_label,
        { file: File -> abs(System.currentTimeMillis() - file.timestamp) < 2592000000 }
    )

    object MoreThanAMonth : TimeFilter(
        R.string.time_filter_more_than_a_month_label,
        { file: File -> abs(System.currentTimeMillis() - file.timestamp) >= 2592000000 }
    )

    data class Custom(
        val timestampStart: Long? = null,
        val timestampEnd: Long? = null
    ) :
        TimeFilter(
//            label = "从 ${timestampStart?.toFormattedDate() ?: "不受限制"} 到 ${timestampEnd?.toFormattedDate() ?: "不受限制"}",
            R.string.time_filter_custom_label,
            fileCheck = { file: File ->
                file.timestamp in (timestampStart ?: System.currentTimeMillis()) until (timestampEnd
                    ?: System.currentTimeMillis())
            }
        )
}

sealed class ExtensionFilter(
    val labelResId: Int,
    val fileCheck: (file: File) -> Boolean
) {
    companion object {
        const val DOCS = 0
        const val SLIDES = 1
        const val SHEETS = 2
        const val PICTURE = 3
        const val VIDEO = 4
        const val AUDIO = 5
        const val COMPRESSED = 6
        const val PDF = 7
    }

    object All : ExtensionFilter(
        R.string.extension_filter_all_label,
        { true }
    )

    object Docs : ExtensionFilter(
        R.string.extension_filter_docs_label,
        { file: File -> file.extension.lowercase() in listOf<String>("doc", "docx", "wps", "wpt") }
    )

    object Slides : ExtensionFilter(
        R.string.extension_filter_slides_label,
        { file: File -> file.extension.lowercase() in listOf<String>("ppt", "pptx", "dps", "dpt") }
    )

    object Sheets : ExtensionFilter(
        R.string.extension_filter_sheets_label,
        { file: File -> file.extension.lowercase() in listOf<String>("xls", "xlsx", "et", "ett") }
    )

    object Picture : ExtensionFilter(
        R.string.extension_filter_picture_label,
        { file: File ->
            file.extension.lowercase() in listOf<String>(
                "bmp",
                "jpeg",
                "jpg",
                "png",
                "tif",
                "gif",
                "pcx",
                "tga",
                "exif",
                "fpx",
                "svg",
                "psd",
                "cdr",
                "pcd",
                "dxf",
                "ufo",
                "eps",
                "ai",
                "raw",
                "webp",
                "avif",
                "apng",
                "tiff"
            )
        }
    )

    object Video : ExtensionFilter(
        R.string.extension_filter_video_label,
        { file: File ->
            file.extension.lowercase() in listOf<String>(
                "avi",
                "wmv",
                "mp4",
                "mpeg",
                "mpg",
                "mov",
                "flv",
                "rmvb",
                "rm",
                "asf"
            )
        }
    )

    object Audio : ExtensionFilter(
        R.string.extension_filter_audio_label,
        { file: File ->
            file.extension.lowercase() in listOf<String>(
                "cd",
                "wav",
                "aiff",
                "mp3",
                "wma",
                "ogg",
                "mpc",
                "flac",
                "ape",
                "3gp",
            )
        }
    )

    object Compressed : ExtensionFilter(
        R.string.extension_filter_compressed_label,
        { file: File ->
            file.extension.lowercase() in listOf<String>(
                "zip",
                "rar",
                "7z",
                "bz2",
                "tar",
                "jar",
                "gz",
                "deb"
            )
        }
    )

    object Pdf : ExtensionFilter(
        R.string.extension_filter_pdf_label,
        { file: File ->
            file.extension.lowercase() in listOf<String>(
                "pdf",
                "epub",
                "mobi",
                "iba",
                "azw"
            )
        }
    )
}

sealed class SizeFilter(
    val labelResId: Int,
    val fileCheck: (file: File) -> Boolean
) {
    object All : SizeFilter(
        R.string.size_filter_all_label,
        { true }
    )

    object TenMB : SizeFilter(
        R.string.size_filter_ten_mb_label,
        { file: File -> file.size < 10000000 }
    )

    object HundredMB : SizeFilter(
        R.string.size_filter_hundred_mb_label,
        { file: File -> file.size in 10000000 until 100000000 }
    )

    object OverHundredMB : SizeFilter(
        R.string.size_filter_over_hundred_mb_label,
        { file: File -> file.size > 100000000 }
    )
}