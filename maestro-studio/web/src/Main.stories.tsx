import React, { useState } from 'react';
import { DeviceScreen } from './models';
import { sampleElements, sampleScreenshot } from './fixtures';
import Main from './Main';

export default {
  title: 'Main',
  parameters: {
    layout: 'fullscreen'
  }
}

const deviceScreen: DeviceScreen = {
  screenshot: sampleScreenshot,
  width: 1080,
  height: 2340,
  elements: sampleElements,
}

export const MainStory = () => {
  const [refreshCount, setRefreshCount] = useState(0)
  const getDeviceScreen = async () => {
    await new Promise(resolve => setTimeout(resolve, 1000))
    setRefreshCount(prev => prev + 1)
    if (refreshCount % 5 === 4) {
      throw new Error("asdf")
    }
    return deviceScreen
  }
  return (
    <Main getDeviceScreen={getDeviceScreen} />
  )
}
