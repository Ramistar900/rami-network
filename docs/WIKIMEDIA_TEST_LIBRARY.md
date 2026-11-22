# Wikimedia Commons VOD test library

This is an owner-triggered test import for the separate `RAMI VOD TEST` channel. It does not modify `RAMI TEST LIVE` and does not upload video files to Supabase. Viewers request the public Commons file URL directly.

All ten selected files are WebM and are labelled Public Domain in the United States on their individual Commons pages. That status is not a blanket legal conclusion for every country; keep this set for technical testing and check local legal requirements before commercial or international broadcasting. Do not use NASA logos, insignia, or wording that implies NASA endorsement.

| Title | Duration | File size | Source / creator | Commons file |
|---|---:|---:|---|---|
| 50 Kilometers of Brazilian Forest Canopy | 10:02 | 86.28 MB | NASA Scientific Visualization Studio | [File page](https://commons.wikimedia.org/wiki/File:50_Kilometers_of_Brazilian_Forest_Canopy.webm) |
| Universe (1976) | 26:52 | 38.06 MB | NASA | [File page](https://commons.wikimedia.org/wiki/File:Universe_(1976).webm) |
| The Challenges of Outer Space – NASA 1955 | 24:13 | 196.14 MB | NASA | [File page](https://commons.wikimedia.org/wiki/File:The_Challenges_of_Outer_Space_NASA_1955.webm) |
| Apollo 6 – Video from Command Module | 15:18 | 35.02 MB | NASA | [File page](https://commons.wikimedia.org/wiki/File:Apollo_6_video_from_Command_Module.webm) |
| Orion Soars on First Flight Test | 10:39 | 122.19 MB | NASA; Commons reviewer confirmed PD status 3 Jan 2017 | [File page](https://commons.wikimedia.org/wiki/File:Orion_Soars_on_First_Flight_Test.webm) |
| Cosmic Cycles 1 – The Sun | 10:40 | 84.14 MB | NASA Scientific Visualization Studio; this copy has no audio | [File page](https://commons.wikimedia.org/wiki/File:Cosmic_Cycles_1-_The_Sun_(SVS14313_-_The_Sun_Concert_50mbps).webm) |
| Gemini XII Mission (1966) | 25:39 | 158.48 MB | NASA via Cape Canaveral Space Force Museum | [File page](https://commons.wikimedia.org/wiki/File:Gemini_XII_Mission_(1966).webm) |
| Legacy of Gemini (1967) | 27:37 | 119.89 MB | NASA | [File page](https://commons.wikimedia.org/wiki/File:Legacy_of_Gemini_(1967).webm) |
| Eat Like an Astronaut | 12:41 | 103.09 MB | NASA Johnson | [File page](https://commons.wikimedia.org/wiki/File:Eat_Like_an_Astronaut.webm) |
| Action Cam Footage from U.S. Spacewalk 41 | 10:21 | 200.22 MB | NASA Johnson | [File page](https://commons.wikimedia.org/wiki/File:Action_Cam_Footage_from_U.S._Spacewalk_-41.webm) |

## Playback notes

- The ten files total approximately 1.14 GB if watched in full; they stream directly from Wikimedia and are not copied to Supabase.
- Most files are VP9 WebM. Actual decoding depends on the device's codecs; test on the target Android 9 TV before treating the library as production-ready.
- Wikimedia Commons is a temporary test origin, not a guaranteed production CDN. Availability, bandwidth, and throttling are controlled by Wikimedia.
- The first batch is space and Earth-science content with English narration where present. It is a technical test playlist, not the final multi-genre RAMİ TV catalogue.
- Source page and rights note are stored with each source record. NASA logos and endorsement are not used.
