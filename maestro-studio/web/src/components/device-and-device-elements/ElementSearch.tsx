import React, { useState, ChangeEvent } from "react";
import { Listbox, Transition } from "@headlessui/react";
import { Input } from "../design-system/input";

const elements = [
  "Hydrogen",
  "Helium",
  "Lithium",
  "Beryllium",
  "Boron",
  "Carbon",
  "Nitrogen",
  "Oxygen",
  "Fluorine",
  "Neon",
]; // list of elements to search

export default function ElementSearch() {
  const [showPopover, setShowPopover] = useState<boolean>(false);
  const [selectedElement, setSelectedElement] = useState<string | null>(null);
  const [visibleElements, setVisibleElements] = useState<string[]>(elements);

  const handleInput = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    const filteredElements = elements.filter((element) =>
      element.toLowerCase().includes(value.toLowerCase())
    );
    setVisibleElements(filteredElements);
  };

  const handleSelect = (element: string) => {
    setSelectedElement(element);
  };

  return (
    <div className="relative">
      <div>
        <Input
          onChange={handleInput}
          onFocus={() => setShowPopover(true)}
          onBlur={() => setShowPopover(false)}
          size="sm"
          leftIcon="RiSearchLine"
          placeholder="Search element by Text or Id"
          className="w-full border border-gray-200 rounded-md"
        />
      </div>

      <Listbox value={selectedElement} onChange={handleSelect}>
        {({ open }) => (
          <Transition
            show={open}
            as="div"
            className="absolute z-10 w-full mt-2 bg-white rounded-md shadow-lg"
          >
            <div className="py-1 text-base leading-6 rounded-md ring-1 ring-black ring-opacity-5">
              {visibleElements.map((element, idx) => (
                <Listbox.Option
                  key={idx}
                  value={element}
                  className={({ active }) =>
                    `${active ? "text-white bg-indigo-600" : "text-gray-900"}
                            cursor-default select-none relative py-2 pl-3 pr-9`
                  }
                >
                  {({ selected }) => (
                    <span
                      className={`${
                        selected ? "font-medium" : "font-normal"
                      } block truncate`}
                    >
                      {element}
                    </span>
                  )}
                </Listbox.Option>
              ))}
            </div>
          </Transition>
        )}
      </Listbox>
    </div>
  );
}
