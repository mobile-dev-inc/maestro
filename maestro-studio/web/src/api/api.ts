import { BannerMessage, DeviceScreen, FormattedFlow, MockEvent, Repl, ReplCommand, } from "../helpers/models";
import useSWR, { mutate, SWRConfiguration, SWRResponse } from "swr";
import useSWRSubscription from 'swr/subscription';

export type ReplResponse = {
  repl?: Repl;
  error?: any;
};

type GetMockDataResponse = {
  projectId: string;
  events: MockEvent[];
};

export class HttpError extends Error {
  constructor(public status: number, public message: string) {
    super(message);
  }
}

export const wait = async (durationMs: number) => {
  return new Promise((resolve) => setTimeout(resolve, durationMs));
};

const makeRequest = async <T>(
  method: string,
  path: string,
  body?: Object | undefined
): Promise<T> => {
  const response = await fetch(path, {
    method,
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    const body = await response.text();
    throw new HttpError(response.status, body);
  }
  return await response.json();
};

const useRepl = (): ReplResponse => {
  const { data: repl, error } = useSWRSubscription<Repl, any, string>('/api/repl/sse', (key, { next }) => {
    const eventSource = new EventSource(key);
    eventSource.onmessage = e => {
      const repl: Repl = JSON.parse(e.data)
      next(null, repl)
    }
    eventSource.onerror = error => {
      next(error)
    }
    return () => eventSource.close()
  })
  return { repl, error };
};

const fetcher = (url: string) => fetch(url).then((r) => r.json());

export const API = {
  useDeviceScreen: (
    config?: SWRConfiguration<DeviceScreen>
  ): SWRResponse<DeviceScreen> => {
    return useSWR(
      "/api/device-screen",
      (url) => makeRequest("GET", url),
      config
    );
  },
  getDeviceScreen: async (): Promise<DeviceScreen> => {
    return makeRequest("GET", `/api/device-screen`);
  },
  useBannerMessage: (
    config?: SWRConfiguration<BannerMessage>
  ): SWRResponse<BannerMessage> => {
    return useSWR(
      "/api/banner-message",
      (url) => makeRequest("GET", url),
      config
    );
  },
  repl: {
    useRepl,
    runCommand: async (yaml: string): Promise<Repl> => {
      return makeRequest("POST", "/api/repl/command", { yaml });
    },
    runCommandsById: async (ids: string[]): Promise<Repl> => {
      return makeRequest("POST", "/api/repl/command", { ids });
    },
    deleteCommands: async (ids: string[]): Promise<Repl> => {
      return makeRequest("DELETE", "/api/repl/command", { ids });
    },
    reorderCommands: async (ids: string[]) => {
      makeRequest("POST", "/api/repl/command/reorder", { ids });
      mutate<Repl>(
        "/api/repl/sse",
        (repl) => {
          if (!repl) return undefined;
          const newCommands: ReplCommand[] = [];
          const idsAsSet = new Set(ids);
          repl.commands
            .filter((c) => !!c && idsAsSet.has(c.id))
            .forEach((c) => newCommands.push(c));
          return { ...repl, commands: newCommands };
        },
        {
          revalidate: false,
        }
      );
    },
    formatFlow: async (ids: string[]): Promise<FormattedFlow> => {
      return makeRequest("POST", "/api/repl/command/format", { ids });
    },
  },
  useMockData: (config?: SWRConfiguration) => {
    return useSWR<GetMockDataResponse>(
      "/api/mock-server/data",
      fetcher,
      config
    );
  },
};
