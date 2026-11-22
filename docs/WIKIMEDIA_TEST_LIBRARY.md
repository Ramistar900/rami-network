# Wikimedia Commons VOD test library

This is a small, owner-triggered test import for the separate `RAMI VOD TEST` channel. It does not modify `RAMI TEST LIVE` and does not upload video files to Supabase. Viewers request the public Commons file URL directly.

| Title | Duration | File size | License / attribution | Source |
|---|---:|---:|---|---|
| Forest walking and outdoor cooking – Slovenia | 1:14:23 | 3.1 GB | CC BY 3.0; attribute Snicel 69; no edits | [Commons file](https://commons.wikimedia.org/wiki/File:Forest_walking_and_outdoor_cooking_slovenia.webm) |
| The Most Beautiful Drone Footage – Cross Country Bicycle Tour | 15:51 | 1.32 GB | CC BY 3.0; attribute Ryan Van Duzer; no edits | [Commons file](https://commons.wikimedia.org/wiki/File:The_Most_Beautiful_Drone_Footage_From_Our_Cross_Country_Bicycle_Tour.webm) |
| 50 Kilometers of Brazilian Forest Canopy | 10:02 | 86.28 MB | Public domain in the United States; NASA Scientific Visualization Studio; no NASA endorsement or logos | [Commons file](https://commons.wikimedia.org/wiki/File:50_Kilometers_of_Brazilian_Forest_Canopy.webm) |
| Universe (1976) | 26:52 | 38.06 MB | Public domain in the United States; NASA; no NASA endorsement or logos | [Commons file](https://commons.wikimedia.org/wiki/File:Universe_(1976).webm) |
| The Challenges of Outer Space – NASA 1955 | 24:13 | 196.14 MB | Public domain in the United States; NASA; no NASA endorsement or logos | [Commons file](https://commons.wikimedia.org/wiki/File:The_Challenges_of_Outer_Space_NASA_1955.webm) |

## Playback notes

- The first two files are large; a full uninterrupted viewing session may consume substantial data.
- WebM is a progressive HTTPS media source. The Android player must support the codecs contained in each file; successful API registration alone does not prove decoding on every device.
- Wikimedia Commons is the temporary test origin, not a guaranteed production CDN. Availability, bandwidth, and throttling are controlled by Wikimedia.
- Public-domain statements for NASA materials above are specifically the Commons/U.S. status. Confirm any additional local legal requirements before commercial or international broadcasting.
- Keep attribution visible in the content details and retain the source page and license URL in the source rights note.
