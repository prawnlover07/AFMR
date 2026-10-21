# Deferred features

## Optional loudness scanner

The user requested that a built-in ReplayGain/loudness scanner be kept for later. The current implementation only reads existing track tags.

When implementing a scanner, make it an explicit, cancellable job with progress. Analyze songs away from the game thread with bounded worker resources; cache results in AFMR-owned data and invalidate only changed files. Do not modify original music or foobar's files, or automatically scan the whole library at launch. Reuse valid existing tags before analyzing audio. Consider playlist/selected-track scans so a large library need not be processed all at once. Benchmark gameplay impact before enabling background analysis during play.
