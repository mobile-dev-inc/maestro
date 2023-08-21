import { useEffect, useRef } from "react";
import InteractableDevice from "./InteractableDevice";
import { UIElement } from "../../helpers/models";
import { useDeviceContext } from "../../context/DeviceContext";
import { Button } from "../design-system/button";

export default function SelectedElementViewer({
  uiElement,
  copyId,
}: {
  uiElement: UIElement | null;
  copyId: () => void;
}) {
  const { deviceScreen } = useDeviceContext();
  const defaultWrapperSize = 320;
  const containerElementRef = useRef<HTMLDivElement>(null);
  const deviceRef = useRef<HTMLDivElement>(null);

  /**
   * Set The element size and position based on Element selected
   */
  useEffect(() => {
    if (!uiElement || !deviceScreen) {
      return;
    }

    if (deviceRef.current && uiElement.bounds) {
      // Set the wrapper height to be used & increase height if element is big
      let currentWrapperHeight = defaultWrapperSize;
      const maxHeight =
        defaultWrapperSize * (deviceScreen.height / deviceScreen.width) - 4;
      // Chromium Screen (Smaller height)
      if (deviceScreen.height < deviceScreen.width) {
        containerElementRef.current &&
          (containerElementRef.current.style.height = `${maxHeight}px`);
        return;
      }
      // Selected element is large in height
      else if (uiElement.bounds.height > deviceScreen.width) {
        currentWrapperHeight = Math.min(
          defaultWrapperSize * (uiElement.bounds.height / deviceScreen.width) +
            5000,
          maxHeight
        );
        containerElementRef.current &&
          (containerElementRef.current.style.height = `${currentWrapperHeight}px`);
      }
      // Current Element Position Ratio
      const selectedRatio =
        (2 * uiElement.bounds.y + uiElement.bounds.height) /
        (2 * deviceScreen.height);
      // Current Calculated top position for center
      const currentTopPos =
        currentWrapperHeight / 2 -
        1 * deviceRef.current.clientHeight * selectedRatio;
      // Max Value of top
      const maxTopValue =
        (-1 *
          deviceRef.current.clientHeight *
          (deviceScreen.height - deviceScreen.width)) /
        deviceScreen.height;
      // Min Value for top
      const minTopValue = 0;
      // Set position
      deviceRef.current.style.top = `${Math.max(
        Math.min(currentTopPos, minTopValue),
        maxTopValue
      )}px`;
    }
  }, [deviceScreen, uiElement]);

  return (
    <div
      className="hidden md:block"
      style={{ width: defaultWrapperSize + "px" }}
    >
      {uiElement?.resourceId && (
        <>
          <p className="text-sm mb-1">Element Id:</p>
          <div className="bg-gray-100 px-3 py-2 rounded-lg mb-4 flex gap-2">
            <p
              className="text-sm font-semibold flex-grow py-1.5"
              style={{ lineBreak: "anywhere" }}
            >
              {uiElement?.resourceId}
            </p>
            {copyId && (
              <Button
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  copyId();
                }}
                tabIndex={-1}
                variant="tertiary"
                size="sm"
                icon="RiFileCopyLine"
              />
            )}
          </div>
        </>
      )}
      <div
        style={{
          minWidth: defaultWrapperSize + "px",
        }}
      >
        <div
          ref={containerElementRef}
          style={{ height: defaultWrapperSize + "px" }}
          className="relative overflow-hidden rounded-lg border border-black/20 dark:border-white/20"
        >
          <div ref={deviceRef} className="absolute -top-1 -left-2 -right-2">
            <InteractableDevice enableGestureControl={false} />
          </div>
        </div>
      </div>
    </div>
  );
}
