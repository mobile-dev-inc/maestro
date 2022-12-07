import { DeviceScreen, UIElement, UIElementBounds } from './models';
import React, { useRef } from 'react';
import useMouse, { MousePosition } from '@react-hook/mouse-position';

type AnnotationState = 'default' | 'hidden' | 'hovered' | 'selected'

const Annotation = ({element, deviceWidth, deviceHeight, state}: {
  element: UIElement
  deviceWidth: number
  deviceHeight: number
  state: AnnotationState
}) => {
  if (!element.bounds) return null
  const {x, y, width, height} = element.bounds
  const l = `${x / deviceWidth * 100}%`
  const t = `${y / deviceHeight * 100}%`
  const w = `${width / deviceWidth * 100}%`
  const h = `${height / deviceHeight * 100}%`
  return (
    <div
      className="absolute border border-dashed border-pink-400"
      style={{ left: l, top: t, width: w, height: h }}
    >{state}</div>
  )
}

const pointInBounds = (boundsX: number, boundsY: number, boundsWidth: number, boundsHeight: number, x: number, y: number) => {
  return (x >= boundsX && x <= boundsX + boundsWidth) && (y >= boundsY && y <= boundsY + boundsHeight)
}

const getHoveredElement = (deviceScreen: DeviceScreen, mouse: MousePosition): UIElement | null => {
  const hoveredList = deviceScreen.elements.filter(element => {
    if (!element.bounds) return false
    const { x: boundsX, y: boundsY, width: boundsWidth, height: boundsHeight } = element.bounds
    const { x: mouseX, y: mouseY, elementWidth, elementHeight } = mouse
    if (mouseX === null || mouseY === null || elementWidth === null || elementHeight === null) return false
    return pointInBounds(
      boundsX / deviceScreen.width,
      boundsY / deviceScreen.height,
      boundsWidth / deviceScreen.width,
      boundsHeight / deviceScreen.height,
      mouseX / elementWidth,
      mouseY / elementHeight,
    )
  })
  if (hoveredList.length === 0) return null
  return hoveredList[0]
}

export const AnnotatedScreenshot = ({deviceScreen}: {
  deviceScreen: DeviceScreen
}) => {
  const ref = useRef(null)
  const mouse = useMouse(ref)
  const hoveredElement = getHoveredElement(deviceScreen, mouse)

  return (
    <div
      ref={ref}
      className="relative h-full bg-red-100"
      style={{
        aspectRatio: deviceScreen.width / deviceScreen.height
      }}
    >
      <img className="h-full" src={deviceScreen.screenshot} alt="screenshot"/>
      <div className="absolute inset-0 bg-white opacity-50"/>
      {deviceScreen.elements.map(element =>{
        let state: AnnotationState = 'default'
        if (hoveredElement === element) {
          state = 'hovered'
        }
        return (
          <Annotation
            element={element}
            deviceWidth={deviceScreen.width}
            deviceHeight={deviceScreen.height}
            state={state}
          />
        )
      })}
    </div>
  );
}