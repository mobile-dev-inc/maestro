import { AnnotatedScreenshot } from './AnnotatedScreenshot';
import React, { useState } from 'react';
import { DeviceScreen, UIElement } from './models';
import PageSwitcher from './PageSwitcher';
import Banner from './Banner';
import ElementSearch from './ElementSearch';
import { motion } from 'framer-motion';

const Inspect = ({ deviceScreen }: {
  deviceScreen: DeviceScreen
}) => {
  const [hoveredElement, setHoveredElement] = useState<UIElement | null>(null)
  const [selectedElement, setSelectedElement] = useState<UIElement | null>(null)
  
  const banner = selectedElement ? (
    <Banner
      left={selectedElement.text || ''}
      right={selectedElement.resourceId || ''}
      onClose={() => setSelectedElement(null)}
    />
  ) : null

  const detailsPage = selectedElement ? (
    <div
      className="font-bold"
    >
      Here are some examples of how you can interact with this element:
    </div>
  ) : null;
  
  return (
    <motion.div
      className="App flex h-full gap-10 p-10 overflow-hidden"
      initial={{ scale: .95, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      transition={{ duration: .1 }}
    >
      <AnnotatedScreenshot
        deviceScreen={deviceScreen}
        onElementHovered={setHoveredElement}
        hoveredElement={hoveredElement}
        onElementSelected={setSelectedElement}
        selectedElement={selectedElement}
      />
      <PageSwitcher banner={banner}>
        <ElementSearch
          deviceScreen={deviceScreen}
          onElementHovered={setHoveredElement}
          hoveredElement={hoveredElement}
          onElementSelected={setSelectedElement}
        />
        {detailsPage}
      </PageSwitcher>
    </motion.div>
  )
}

export default Inspect