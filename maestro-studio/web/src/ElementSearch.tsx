import React, { useState } from 'react';
import { DeviceScreen, UIElement } from './models';

const compare = (a: string | undefined, b: string | undefined) => {
  if (!a) return b ? 1 : 0
  if (!b) return -1
  return a.localeCompare(b)
}

const SearchIcon = (props: React.SVGProps<SVGSVGElement>) => {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" {...props}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
    </svg>
  )
}

const ElementSearch = ({deviceScreen, onElementSelected, hoveredElement, onElementHovered}: {
  deviceScreen: DeviceScreen
  onElementSelected:  React.Dispatch<React.SetStateAction<UIElement | null>>
  hoveredElement: UIElement | null
  onElementHovered: React.Dispatch<React.SetStateAction<UIElement | null>>
}) => {
  const [query, setQuery] = useState("");

  const filteredElements = deviceScreen.elements.filter(element => {
    return !query || element.text?.toLowerCase().includes(query.toLowerCase()) || element.resourceId?.toLowerCase().includes(query.toLowerCase())
  })

  const sortedElements = filteredElements.sort((a, b) => {
    const aTextPrefixMatch = query && a.text?.toLowerCase().startsWith(query.toLowerCase())
    const bTextPrefixMatch = query && b.text?.toLowerCase().startsWith(query.toLowerCase())
    if (aTextPrefixMatch && !bTextPrefixMatch) return -1
    if (bTextPrefixMatch && !aTextPrefixMatch) return 1
    return compare(a.text, b.text) || compare(a.resourceId, b.resourceId)
  })

  return (
    <div className="flex flex-col h-full gap-3">
      <div className="font-bold text-lg mb-5">UI Elements</div>
      <div className="flex relative">
        <div className="flex items-center pl-5 absolute pointer-events-none inset-y-0">
          <SearchIcon className="w-6 text-slate-500"/>
        </div>
        <input
          type="search"
          placeholder="Search by element text or ID"
          className="rounded w-full p-4 pl-14 bg-slate-100 focus:outline-slate-900"
          value={query}
          onChange={e => setQuery(e.target.value)}
        />
      </div>
      <div className="flex flex-col overflow-scroll ">
        {sortedElements.map(element => (
          <div
            className="pb-1"
            key={element.id}
            onClick={() => {
              onElementSelected(element)
            }}
            onMouseOver={() => {
              onElementHovered(element)
            }}
            onMouseLeave={() => {
              onElementHovered(prev => prev === element ? null : prev)
            }}
          >
            <div
              className={`flex gap-3 p-5 items-center rounded border ${hoveredElement === element ? 'bg-slate-100' : ''} active:bg-slate-200`}
            >
              <span className="whitespace-nowrap overflow-hidden text-ellipsis cursor-default">{element.text}</span>
              <div className="flex-1"/>
              <span className="whitespace-nowrap overflow-hidden text-ellipsis cursor-default">{element.resourceId}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default ElementSearch
