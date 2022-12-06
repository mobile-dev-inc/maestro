import { DeviceScreen, UIElement, UIElementBounds } from './models';
import React from 'react';

const clamp = (n: number, min: number, max: number) => {
  return Math.max(Math.min(n, max), min)
}

const clampBounds = (bounds: UIElementBounds) => {
  let {x: l, y: t, width, height} = bounds
  let r = l + width
  let b = t + height

  l = clamp(l, 0, 1)
  t = clamp(t, 0, 1)
  r = clamp(r, l, 1)
  b = clamp(b, t, 1)

  width = r - l
  height = b - t

  return { x: l, y: t, width, height}
}

const Annotation = ({element, deviceWidth, deviceHeight}: {
  element: UIElement
  deviceWidth: number
  deviceHeight: number
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
    ></div>
  )
}

export const AnnotatedScreenshot = ({deviceScreen}: {
  deviceScreen: DeviceScreen
}) => {
  return (
    <div
      className="relative h-full bg-red-100"
      style={{
        aspectRatio: deviceScreen.width / deviceScreen.height
      }}
    >
      <img className="h-full" src={deviceScreen.screenshot} alt="screenshot"/>
      <div className="absolute inset-0 bg-white opacity-50"/>
      {deviceScreen.elements.map(element => (
        <Annotation
          element={element}
          deviceWidth={deviceScreen.width}
          deviceHeight={deviceScreen.height}
        />
      ))}
    </div>
  );
}