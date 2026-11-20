package indi.dmzz_yyhyy.lightnovelreader.data.web

import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.identifier.Identifier

/**
 * 把数据源返回的卷/章节 id 统一加上数据源前缀，便于跨源共存。
 */
fun qualifyBookVolumes(
    sourceId: Identifier,
    localBookId: String,
    volumes: BookVolumes
): BookVolumes = volumes.copy(
    bookId = localBookId,
    volumes = volumes.volumes.map { volume ->
        volume.copy(
            volumeId = qualifyId(sourceId, volume.volumeId),
            chapters = volume.chapters.map { chapter ->
                chapter.copy(id = qualifyId(sourceId, chapter.id))
            }
        )
    }
)

fun qualifyChapterContent(
    sourceId: Identifier,
    localChapterId: String,
    content: ChapterContent
): ChapterContent = content.copy(
    id = localChapterId,
    prevChapter = content.prevChapter?.let { qualifyId(sourceId, it) },
    nextChapter = content.nextChapter?.let { qualifyId(sourceId, it) }
)

/**
 * 调用插件/数据源前，把本地 id 还原成远端认识的 id。
 */
fun unqualifyBookVolumes(sourceId: Identifier, volumes: BookVolumes): BookVolumes =
    volumes.copy(
        volumes = volumes.volumes.map { volume ->
            volume.copy(
                volumeId = stripSourcePrefix(sourceId, volume.volumeId),
                chapters = volume.chapters.map { chapter ->
                    chapter.copy(id = stripSourcePrefix(sourceId, chapter.id))
                }
            )
        }
    )
