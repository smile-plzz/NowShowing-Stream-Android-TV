package com.nowshowing.tv.playback

sealed interface EpisodeStep {
    data class Play(val season: Int, val episode: Int) : EpisodeStep
    data class LoadSeason(val season: Int) : EpisodeStep
    data object Complete : EpisodeStep
    data object Unavailable : EpisodeStep
}

/** Never infer an episode number or season boundary from missing metadata. */
fun nextEpisodeStep(season: Int, episode: Int, totalSeasons: Int, knownEpisodes: List<Int>): EpisodeStep {
    val numbers = knownEpisodes.filter { it > 0 }.distinct().sorted()
    if (season < 1 || episode !in numbers || totalSeasons < season) return EpisodeStep.Unavailable
    numbers.firstOrNull { it > episode }?.let { return EpisodeStep.Play(season, it) }
    return if (season < totalSeasons) EpisodeStep.LoadSeason(season + 1) else EpisodeStep.Complete
}
