import { AnnotatedScreenshot } from './AnnotatedScreenshot';
import React, { useState } from 'react';
import { DeviceScreen, UIElement } from './models';
import PageSwitcher from './PageSwitcher';

const CloseIcon = () => {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
    </svg>
  )
}

export const Banner = ({left, right, onClick}: {
  left: string
  right: string
  onClick: () => void
}) => {
  return (
    <div className="flex justify-between items-center font-bold p-2 pr-5 rounded bg-blue-100 border border-blue-500" onClick={onClick}>
      <div className="flex gap-3 items-center">
        <div className="flex justify-center p-2 rounded items-center hover:bg-blue-900/20 active:bg-blue-900/40">
          <CloseIcon />
        </div>
        <span>{left}</span>
      </div>
      <span>{right}</span>
    </div>
  )
}

const Inspect = ({ deviceScreen }: {
  deviceScreen: DeviceScreen
}) => {
  const [hoveredElement, setHoveredElement] = useState<UIElement | null>(null)
  const [selectedElement, setSelectedElement] = useState<UIElement | null>(null)
  
  const banner = selectedElement ? (
    <Banner
      left={selectedElement.text || ''}
      right={selectedElement.resourceId || ''}
      onClick={() => setSelectedElement(null)}
    />
  ) : null

  const searchPage = (
    <div
      className="font-bold text-lg"
    >
      UI Elements
    </div>
  )

  const detailsPage = selectedElement ? (
    <div
      className="font-bold"
    >
      Here are some examples of how you can interact with this element:
    </div>
  ) : null;
  
  return (
    <div className="App flex h-full gap-10">
      <AnnotatedScreenshot
        deviceScreen={deviceScreen}
        onElementHovered={setHoveredElement}
        hoveredElement={hoveredElement}
        onElementSelected={setSelectedElement}
        selectedElement={selectedElement}
      />
      <PageSwitcher banner={banner}>
        {searchPage}
        {detailsPage}
      </PageSwitcher>
    </div>
  )
}

export default Inspect