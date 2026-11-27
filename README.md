# Location Camera Points

Save camera positions for locations and restore them when you return.

Each camera point has a location, a radius, a direction (north/east/south/west) and a zoom level.
When you walk into a point's area, the camera turns and zooms to match. It only changes when you
*enter* an area, so you can still move the camera freely while you're inside it. You can create as
many points as you like.

## Usage

- **Add a point:** open the *Location Camera Points* side panel, set up your camera, and click
  **Add point at my location**. You can also shift right-click any tile and choose **Add camera point**.
  New points start with your current facing (rounded to the nearest direction) and your current zoom.
- **Edit a point:** each point in the panel has its own name, X/Y/plane, radius, facing, and zoom.
  Changes save immediately.
  - *Move to my location* moves the point to where you're standing.
  - *Current* (next to zoom) fills in your current zoom.
  - Uncheck *Zoom*, or set facing to *Don't change*, to leave that part of the camera alone.
  - Use the checkbox next to the name to turn a point on or off without deleting it.
- **Test** applies a point's camera right away. ▲/▼ reorder the list.
- If areas overlap, the point whose centre is closest to you is used.

## Settings

| Setting | Description |
|---|---|
| Default radius | Radius given to new points |
| Chat message on trigger | Prints a game message when a point changes the camera |
| Show trigger areas | Highlights each enabled point's area in the world |
| Area color | Highlight color |
| Shift right-click menu | Toggles the *Add camera point* tile option |
