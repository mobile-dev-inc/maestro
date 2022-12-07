import { DeviceScreen, UIElement } from './models';
import React from 'react';

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
          state="default"
        />
      ))}
    </div>
  );
}