import React from 'react';
import { DeviceScreen } from './models';
import { sampleElements, sampleScreenshot } from './fixtures.';
import Inspect from './Inspect';

export default {
  title: 'Inspect'
}

const deviceScreen: DeviceScreen = {
  screenshot: sampleScreenshot,
  elements: sampleElements,
}

export const Main = () => {
  return (
    <Inspect deviceScreen={deviceScreen} />
  )
}
