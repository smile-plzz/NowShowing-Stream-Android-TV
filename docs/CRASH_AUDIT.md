# Crash and recommendation audit - 22 October 2026

Baseline: 045cdfd2ef9f6ec5475a1b6af1f3286be5865586.

## Confirmed code defects and fixes

- WebEmbedScreen had no onRenderProcessGone handler. Android's default behavior can terminate the host when the renderer dies. The handler now detaches/destroys the dead view once, clears references and shows explicit Retry/source recovery. No automatic reload loop. This is a confirmed crash path, not proof that it caused the owner's specific incidents.
- Gson can deserialize null into Kotlin non-null fields. Required title fields are validated before caching/displaying detail records, including stale cached candidates.
- Mood loading had no exception recovery; Home/Browse likewise allowed load failures to escape their coroutines. These now show retryable states and preserve cancellation.
- Player focus requests can race removal of focus targets. Requests are guarded, and disposed renderer hosts ignore input/lifecycle actions.
- Optional startup cache pruning could crash the process on storage failure. Its error is now recorded without blocking startup.

## Mood defects

Most rules required adjective-like plot phrases (e.g. motivational) absent from normal plot summaries. Many moods shared four generic seeds. Every mood now has explicit editorial IDs, optionally overridden by the cloud catalog. Editorial evidence can satisfy missing plot keywords but never bypass genre exclusions. Romantic requires Romance instead of accepting any Drama/Comedy. Text mood matching uses word boundaries.

## Evidence still needed

No device crash trace was supplied and no physical TV is attached. The exact observed crash cause remains unconfirmed. A bounded local last-event record now captures exception class/app stack frames or renderer termination category; exception messages/URLs/keys/history are omitted. It appears in Settings diagnostics and photo reports. It does not capture all native crashes, ANRs or OS process kills, and it does not upload anything.

After a recurrence, reopen Settings  App health, freeze a report and photograph the last-event/environment pages. Note which screen/action preceded the close and the installed build.

Automated build/test results are reported by the pull request's GitHub Actions run. Real-TV renderer recovery, navigation and memory acceptance remain required.

