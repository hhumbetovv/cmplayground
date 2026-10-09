package az.theternal.samples.feed

import az.theternal.mvi.ContentState
import az.theternal.mvi.test.StoreTest
import az.theternal.mvi.test.storeTest
import az.theternal.samples.data.FakePostRepository
import az.theternal.samples.data.ReportReason
import az.theternal.samples.report.ReportIntent
import az.theternal.samples.report.ReportState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class FeedLogicTest {
    private val posts = FakePostRepository(latency = 1_000)

    /** The feed loads on start; the tests begin once it has. */
    private fun StoreTest<FeedState, FeedIntent, FeedEffect>.loaded() {
        assertIs<ContentState.Loading<*>>(nextState().posts)
        advance(1_000)
        assertIs<ContentState.Success<*>>(nextState().posts)
    }

    @Test
    fun aSentReportClosesTheSheetAndSaysSo() = runTest {
        storeTest(FeedState(), FeedLogic(posts)) {
            loaded()
            send(FeedIntent.OpenReport("p1"))
            expectState { copy(report = ReportState()) }

            send(FeedIntent.Report(ReportIntent.SelectReason(ReportReason.Spam)), FeedIntent.Report(ReportIntent.Send))
            expectState { copy(report = report?.copy(reason = ReportReason.Spam)) }
            expectState { copy(report = report?.copy(isSending = true)) }

            // The report's output: the feed closes the sheet, then tells the screen.
            advance(1_000)
            expectState { copy(report = null) }
            expectEffect(FeedEffect.Message("Report sent"))
        }
    }

    @Test
    fun closingTheSheetCancelsAReportBeingSent() = runTest {
        storeTest(FeedState(), FeedLogic(posts)) {
            loaded()
            send(
                FeedIntent.OpenReport("p1"),
                FeedIntent.Report(ReportIntent.SelectReason(ReportReason.Spam)),
                FeedIntent.Report(ReportIntent.Send),
            )
            skip(3)
            send(FeedIntent.CloseReport, FeedIntent.Report(ReportIntent.EditComment("too late")))
            expectState { copy(report = null) }

            // No "sent", no write into a sheet that is gone.
            advance(5_000)
        }
    }

    @Test
    fun aFailedReportKeepsTheSheetOpen() = runTest {
        storeTest(FeedState(), FeedLogic(posts)) {
            loaded()
            send(
                FeedIntent.OpenReport("p1"),
                FeedIntent.Report(ReportIntent.SelectReason(ReportReason.Other)),
                FeedIntent.Report(ReportIntent.EditComment("this will ${FakePostRepository.FAILING_TEXT}")),
                FeedIntent.Report(ReportIntent.Send),
            )
            skip(4)
            advance(1_000)
            expectEffect(FeedEffect.Message("The report could not be sent"))
            expectState { copy(report = report?.copy(isSending = false)) }
        }
    }
}
