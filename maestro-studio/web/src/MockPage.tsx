import { useMemo, useState } from "react"
import { MockEvent } from "./models"
import { formatDistance } from 'date-fns'
import { JsonViewer } from "@textea/json-viewer"
import useSWR from 'swr';

const getStatusCodeColor = (statusCode: number): string => {
  if (statusCode < 400) return 'text-green-600'
  if (statusCode < 500) return 'text-orange-600'
  return 'text-red-600'
}

type GetMockDataResponse = {
  projectId: string,
  events: MockEvent[]
}

const useMockData = () => {
  const fetcher = (url: string) => fetch(url).then(r => r.json())
  return useSWR<GetMockDataResponse>('/api/mock-server/data', fetcher)
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
    <div className="flex flex-col px-4 max-h-full">
      <div className="flex flex-row">
        <p>Project Id: {data?.projectId || 'unknown'}</p>
      </div>
      <div className="flex flex-row pt-2 max-h-full">
        <div className="flex flex-col w-full basis-2/4">
          <h1 className="text-lg">Events</h1>
          <input
            type="search"
            placeholder="Filter events by session id, path, status code, response"
            className="rounded w-full p-4 pl-2 my-2 bg-slate-100 focus:outline-slate-900"
            value={query}
            onChange={e => setQuery(e.target.value)}
          />
          <div className="h-[600px] overflow-y-scroll">
            {!data?.events || filteredEvents?.length === 0 ? <p className="text-md">No events found</p> : null}

            {(filteredEvents || []).map(event => (
              <div
                className={`flex flex-col my-2 px-4 rounded-md w-full py-2 cursor-pointer text-gray-600 ${selectedEvent !== event ? 'hover:bg-gray-200' : ''} active:bg-gray-300 ${selectedEvent === event ? 'bg-gray-300' : 'bg-gray-100 '}`}
                onClick={() => setSelectedEvent(event)}
              >
                <span className="text-sm text-gray-500">Session id: {event.sessionId}</span>
                <div className="flex flex-row justify-between space-x-4 my-1">
                  <span className="grow">{event.path}</span>
                  <span className={`${!!event.matched ? 'font-bold' : ''}`}>{!!event.matched ? 'matched' : 'unmatched'}</span>
                  <span className={`${getStatusCodeColor(event.statusCode)} font-bold`}>{event.statusCode}</span>
                </div>
                <span className={"text-xs text-gray-500"}>{formatDistance(new Date(event.timestamp), new Date(), { addSuffix: true })}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-col basis-2/4 px-4">
          <h1 className="text-lg">Response</h1>
          {!selectedEvent ? <p>No event selected</p> : null}
          {!!selectedEvent ? (
            <JsonViewer 
            value={selectedEvent.response || {}}
            theme="dark"
            displayDataTypes={false}
            displayObjectSize={false}
            defaultInspectDepth={6}
            rootName={false}
            style={{padding: 20, fontSize: '1.25em'}}
            />
          ) : null}
        </div>
      </div>
    </div>
  )
}

export default MockPage