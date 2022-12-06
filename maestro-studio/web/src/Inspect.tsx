import { AnnotatedScreenshot } from './AnnotatedScreenshot';
import React from 'react';
import { DeviceScreen } from './models';

const Inspect = ({ deviceScreen }: {
  deviceScreen: DeviceScreen
}) => {
  return (
    <div className="App flex h-full">
      <AnnotatedScreenshot
        className="h-full"
        deviceScreen={deviceScreen}
      />
    </div>
  )
}

export default Inspect