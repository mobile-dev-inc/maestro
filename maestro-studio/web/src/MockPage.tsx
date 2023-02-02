import { useMemo, useState } from "react"
import { MockEvent } from "./models"
import { formatDistance } from 'date-fns'
import { JsonViewer } from "@textea/json-viewer"
import useSWR from 'swr';
import { CodeSnippet } from "./Examples";

const getStatusCodeColor = (statusCode: number): string => {
  if (statusCode < 400) return 'text-green-600'
  if (statusCode < 500) return 'text-orange-600'
  return 'text-red-600'
}

const getMockMethod = (method: string): string => {
  switch (method) {
    case 'POST':
      return 'post'

    case 'GET':
    default:
        return 'get'
  }
}

type GetMockDataResponse = {
  projectId: string,
  events: MockEvent[]
}

const useMockData = () => {
  const fetcher = (url: string) => fetch(url).then(r => r.json())
  return useSWR<GetMockDataResponse>('/api/mock-server/data', fetcher)
}

const safeParse = (res: any, fallback: Object): Object => {
  if (typeof res === 'object') return res

  try {
    const parsed = JSON.parse(res);
    return parsed;
  } catch (err) {
    return fallback
  }
}

const MockPage = () => {
  const [selectedEvent, setSelectedEvent] = useState<MockEvent | undefined>()
  const [query, setQuery] = useState<string>('')
  const {data} = useMockData()

  const filteredEvents = useMemo(() => {
    return data?.events?.filter(event => {
      return (`${event.statusCode}`).includes(query) 
        || event.sessionId.includes(query) 
        || event.path.includes(query) 
        || JSON.stringify(event.response 
        || {}).includes(query)
        || (event.matched === true && query === 'matched')
        || (event.matched === false && query === 'unmatched')
    })
  }, [data?.events, query])

  return (
    <div className="flex flex-col px-8 max-h-full overflow-hidden">
      <div className="flex flex-col gap-4 font-mono p-2 my-4 bg-blue-50 rounded-md border border-blue-400 text-blue-900">
        <p>Project Id: {data?.projectId || 'unknown'}</p>
      </div>
      <div className="flex flex-row pt-2 max-h-full overflow-scroll">
        <div className="flex flex-col w-full basis-2/4 overflow-hidden">
          <h1 className="text-lg font-bold">Events</h1>
          <input
            type="search"
            placeholder="Filter events by session id, path, status code, response"
            className="rounded w-full p-4 pl-2 my-2 bg-slate-100 focus:outline-slate-900"
            value={query}
            onChange={e => setQuery(e.target.value)}
          />
          <div className="h-100 overflow-y-scroll">
            {!data?.events || filteredEvents?.length === 0 ? <p className="text-md">No events found</p> : null}

            {(filteredEvents || []).map(event => (
              <div
                className={`flex flex-col my-2 px-4 rounded-md w-full py-2 cursor-pointer text-gray-600 ${selectedEvent !== event ? 'hover:bg-gray-200' : ''} active:bg-gray-300 ${selectedEvent === event ? 'bg-gray-300' : 'bg-gray-100 '}`}
                onClick={() => setSelectedEvent(event)}
              >
                <span className="text-sm text-gray-500">Session id: {event.sessionId}</span>
                <div className="flex flex-row justify-between space-x-4 my-1">
                  <span className="grow">{event.method} {event.path}</span>
                  <span className={`${!!event.matched ? 'font-bold' : ''}`}>{!!event.matched ? 'matched' : 'unmatched'}</span>
                  <span className={`${getStatusCodeColor(event.statusCode)} font-bold`}>{event.statusCode}</span>
                </div>
                <span className={"text-xs text-gray-500"}>{formatDistance(new Date(event.timestamp), new Date(), { addSuffix: true })}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-col basis-2/4 px-8 overflow-hidden">
          <h1 className="text-lg font-bold">{!!selectedEvent?.matched ? 'Mocked response' : 'Response'}</h1>
          {!selectedEvent ? <p className="text-slate-500">No event selected</p> : (
            <>
              <JsonViewer 
                value={safeParse(selectedEvent.response, {})}
                theme="dark"
                displayDataTypes={false}
                displayObjectSize={false}
                defaultInspectDepth={6}
                rootName={false}
                style={{padding: 20, fontSize: '1.25em', maxHeight: 800, overflowY: 'scroll', minHeight: '25%'}}
              />

              <h1 className="text-lg font-bold mt-12 mb-2">Here are some examples of how you can mock this network call:</h1>
              <div className="h-100 overflow-y-scroll space-y-4 pb-2">
                <div>
                  <span className="text-slate-500 whitespace-nowrap">Basic</span>
                  <CodeSnippet>
                    {`${getMockMethod(selectedEvent.method)}('${selectedEvent.path}', (req, res, session) => {\n\tres.status(${selectedEvent.statusCode}).json({\n\t\tmockedResponse: true\n\t});\n});`}
                  </CodeSnippet>
                </div>

                <div>
                  <span className="text-slate-500 whitespace-nowrap">Use session</span>
                  <CodeSnippet>
                    {`${getMockMethod(selectedEvent.method)}('${selectedEvent.path}', (req, res, session) => {\n\tsession.count = (session.count || 0) + 1;\n\n\tres.status(${selectedEvent.statusCode}).json({\n\t\tmockedResponse: true,\n\t\tcount: session.count\n\t});\n});`}
                  </CodeSnippet>
                </div>

                <div>
                  <span className="text-slate-500 whitespace-nowrap">Decorate original response</span>
                  <CodeSnippet>
                    {`${getMockMethod(selectedEvent.method)}('${selectedEvent.path}', async (req, res, session) => {\n\tconst originalRes = await req.propagate();\n\tconst data = originalRes.json();\n\n\tres.status(${selectedEvent.statusCode}).json({\n\t\t...data,\n\t\tmockedResponse: true\n\t});\n});`}
                  </CodeSnippet>
                </div>
              </div>
          </>
          )}
        </div>
      </div>
    </div>
  )
}

export default MockPage