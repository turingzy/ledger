package cn.turingzy.ledger.data.repository.settings

import android.graphics.Bitmap
import android.net.Uri
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import kotlinx.coroutines.flow.Flow

/**
 * 汇总卡片外观设置数据仓库接口。
 *
 * 管理账本与钱包汇总卡片的背景图片，
 * 支持两阶段暂存-应用背景图片工作流以及位图解码。
 */
interface LedgerCardAppearanceRepository {
    /**
     * 获取指定目标卡片的外观设置流。
     *
     * @param target 目标卡片类型（账本概览卡或钱包概览卡），默认为 [SummaryCardTarget.LEDGER]
     * @return 包含背景文件名的外观模型流
     */
    fun appearance(target: SummaryCardTarget = SummaryCardTarget.LEDGER): Flow<LedgerCardAppearance>

    /**
     * 重置指定目标卡片的背景图片为默认，并清理旧文件。
     *
     * @param target 目标卡片类型，默认为 [SummaryCardTarget.LEDGER]
     */
    suspend fun resetBackground(target: SummaryCardTarget = SummaryCardTarget.LEDGER)

    /**
     * 解码并预处理选中的背景图片，保存到临时缓存文件中。
     *
     * @param uri 图片的 Content 或 File URI
     * @param target 目标卡片类型，默认为 [SummaryCardTarget.LEDGER]
     * @return 预存的临时文件名
     */
    suspend fun prepareBackground(uri: Uri, target: SummaryCardTarget = SummaryCardTarget.LEDGER): String

    /**
     * 放弃并删除先前预存的临时背景文件。
     *
     * @param filename 临时文件名
     */
    suspend fun discardPreparedBackground(filename: String)

    /**
     * 将预存的临时背景图片正式应用并持久化为目标卡片的背景，同时清理旧背景。
     *
     * @param filename 预存的临时文件名
     * @param target 目标卡片类型，默认为 [SummaryCardTarget.LEDGER]
     */
    suspend fun applyPreparedBackground(filename: String, target: SummaryCardTarget = SummaryCardTarget.LEDGER)

    /**
     * 从持久化目录加载并解码背景图片为 [Bitmap]。
     *
     * @param filename 背景文件名
     * @return 解码后的位图对象
     */
    suspend fun loadBackground(filename: String): Bitmap
}
