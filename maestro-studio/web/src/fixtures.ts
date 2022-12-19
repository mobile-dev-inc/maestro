import { DeviceScreen, Repl } from './models';
import { sampleElements } from './sampleElements';
import { Api } from './api';

export const sampleScreenshot = '/sample-screenshot.png'

export const sampleDeviceScreen: DeviceScreen = {
  screenshot: sampleScreenshot,
  width: 1080,
  height: 2340,
  elements: sampleElements,
}

const sampleRepl: Repl = {
  commands: [
    { id: '1', yaml: '- inputText: hello', status: 'success' },
    { id: '2', yaml: '- inputText:\n    text: "hello"', status: 'success' },
    { id: '3', yaml: '- tapOn:\n    id: buttonId', status: 'error' },
    { id: '3', yaml: '- inputText: hello', status: 'canceled' },
    { id: '4', yaml: '- inputText: hello', status: 'running' },
    { id: '4', yaml: '- inputText: hello', status: 'pending' },
  ]
}

export const fakeApi: Api = {
  getDeviceScreen: async () => sampleDeviceScreen,
  repl: {
    useRepl: () => ({
      data: sampleRepl,
      error: undefined,
      isLoading: false,
      isValidating: false,
      mutate: async () => { return undefined }
    }),
    runCommand: async () => sampleRepl,
    deleteCommands: async () => sampleRepl,
    runCommandsById: async () => sampleRepl,
  }
}