import { DivProps, DeviceScreen } from './models';
import React from 'react';

export const AnnotatedScreenshot = ({deviceScreen, ...rest}: {
  deviceScreen: DeviceScreen
} & DivProps) => {
  return (
    <div {...rest}>
      <img className="h-full" src={deviceScreen.screenshot} alt="screenshot"/>
    </div>
  )
}