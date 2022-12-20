import { DeviceScreen, Repl } from './models';
import useSWR, { mutate } from 'swr';
import { useRef } from 'react';

export type ReplResponse = {
  repl?: Repl | undefined
  error?: any
}

const makeRequest = async <T>(method: string, path: string, body?: Object | undefined): Promise<T> => {
  const response = await fetch(path, {
    method,
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) {
    const body = await response.text()
    throw new Error(`Request failed ${response.status}: ${body}`)
  }
  return await response.json()
}

const useRepl = (): ReplResponse => {
  const version = useRef(-1)
  // Long-poll watch endpoint by immediately triggering a refresh
  const {data, error} = useSWR<Repl>('/api/repl/watch', async (url) => {
    const repl: Repl = await makeRequest('GET', `${url}?currentVersion=${version.current}`)
    version.current = repl.version
    // Immediate trigger a refresh, and use the current data as the cache to avoid loading state
    mutate('/api/repl/watch', repl)
    return repl
  })
  return { repl: data, error }
}

export const API = {
  getDeviceScreen: async (): Promise<DeviceScreen> => {
    return makeRequest('GET', '/api/device-screen')
  },
  repl: {
    useRepl,
    runCommand: async (yaml: string): Promise<Repl> => {
      return makeRequest('POST', '/api/repl/command', { yaml })
    },
    runCommandsById: async (ids: string[]): Promise<Repl> => {
      return makeRequest('POST', '/api/repl/command', { ids })
    },
    deleteCommands: async (ids: string[]): Promise<Repl> => {
      return makeRequest('DELETE', '/api/repl/command', { ids })
    }
  }
}
