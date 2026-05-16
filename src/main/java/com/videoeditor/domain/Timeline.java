package com.videoeditor.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Timeline containing multiple tracks (video and audio).
 */
public class Timeline {
    private final List<Track> tracks;

    public Timeline() {
        this.tracks = new ArrayList<>();
        // Create default video and audio tracks
        this.tracks.add(new Track(TrackType.VIDEO));
        this.tracks.add(new Track(TrackType.AUDIO));
    }

    // For JSON deserialization
    public Timeline(List<Track> tracks) {
        this.tracks = tracks != null ? new ArrayList<>(tracks) : new ArrayList<>();
    }

    public List<Track> getTracks() {
        return new ArrayList<>(tracks);
    }

    public Track getVideoTrack() {
        for (Track track : tracks) {
            if (track.getType() == TrackType.VIDEO) {
                return track;
            }
        }
        // Create if doesn't exist
        Track videoTrack = new Track(TrackType.VIDEO);
        tracks.add(videoTrack);
        return videoTrack;
    }

    public Track getAudioTrack() {
        for (Track track : tracks) {
            if (track.getType() == TrackType.AUDIO) {
                return track;
            }
        }
        // Create if doesn't exist
        Track audioTrack = new Track(TrackType.AUDIO);
        tracks.add(audioTrack);
        return audioTrack;
    }

    public void addTrack(Track track) {
        tracks.add(track);
    }

    public void removeTrack(Track track) {
        tracks.remove(track);
    }

    public int getTrackCount() {
        return tracks.size();
    }

    @Override
    public String toString() {
        return "Timeline{tracks=" + tracks.size() + "}";
    }
}
