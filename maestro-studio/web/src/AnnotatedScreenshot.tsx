import { DeviceScreen } from './models';
import React from 'react';

export const AnnotatedScreenshot = ({deviceScreen}: {
  deviceScreen: DeviceScreen
}) => {
  return (
    <div
      className="relative h-full"
      style={{
        aspectRatio: deviceScreen.width / deviceScreen.height
      }}
    >
      <img className="h-full" src={deviceScreen.screenshot} alt="screenshot"/>
    </div>
  )
}