package az.theternal.samples.report

import az.theternal.mvi.test.storeTest
import az.theternal.samples.data.FakePostRepository
import az.theternal.samples.data.ReportReason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ReportLogicTest {
    private val posts = FakePostRepository(latency = 1_000)

    @Test
    fun aSentReportSaysSo() = runTest {
        storeTest(ReportState(), ReportLogic("p1", posts)) {
            send(ReportIntent.SelectReason(ReportReason.Spam))
            expectState { copy(reason = ReportReason.Spam) }
            send(ReportIntent.Send)
            expectState { copy(isSending = true) }
            advance(1_000)
            expectEffect(ReportEffect.Sent)
            expectState { copy(isSending = false) }
        }
    }

    @Test
    fun aFailedReportSaysWhy() = runTest {
        storeTest(ReportState(), ReportLogic("p1", posts)) {
            send(ReportIntent.SelectReason(ReportReason.Spam), ReportIntent.EditComment(FakePostRepository.FAILING_TEXT))
            expectState { copy(reason = ReportReason.Spam) }
            expectState { copy(comment = FakePostRepository.FAILING_TEXT) }
            send(ReportIntent.Send)
            expectState { copy(isSending = true) }
            advance(1_000)
            expectEffect(ReportEffect.Failed("The report could not be sent"))
            expectState { copy(isSending = false) }
        }
    }

    @Test
    fun somethingElseNeedsAComment() = runTest {
        storeTest(ReportState(), ReportLogic("p1", posts)) {
            send(ReportIntent.SelectReason(ReportReason.Other), ReportIntent.Send)
            // The send is ignored: no request, no effect.
            expectState { copy(reason = ReportReason.Other) }
        }
    }
}
