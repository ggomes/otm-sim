package core.geometry;

/**
 * Defines an inner/middle/outter handedness.
 * Can be used for the absolute position of a lane group (or lane) -- meaning the inner addlane, a full lane, or an outer
 * addlane; or the relative position -- meaning toward the inner, neither, or outer.
 */
public enum Side {
    in, middle, out
}
