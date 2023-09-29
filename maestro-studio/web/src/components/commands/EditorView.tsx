import Editor from "@monaco-editor/react";
import { useEffect, useState } from "react";
import { Icon } from "../design-system/icon";
import { Button } from "../design-system/button";

const directoryTree: FolderNode = {
  name: "root",
  isDirectory: true,
  children: [
    {
      name: "folder1",
      absolututePath: "",
      isDirectory: true,
      children: [
        { name: "file1.txt", isDirectory: false } as FileNode,
        { name: "file2.txt", isDirectory: false } as FileNode,
      ],
    } as FolderNode,
    {
      name: "folder2",
      absolututePath: "",
      isDirectory: true,
      children: [{ name: "file3.txt", isDirectory: false } as FileNode],
    } as FolderNode,
    { name: "file4.txt", isDirectory: false } as FileNode,
  ],
};

const EditorView = () => {
  const [code, setCode] = useState("");
  const [file, setFile] = useState();

  const handleFileChange = (event: any) => {
    if (event.target.files) {
      setFile(event.target.files[0]);
      console.log(event.target.files[0]);
    }
  };

  useEffect(() => {
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        if (e.target !== null) {
          if (e.target.result !== null) {
            setCode(e.target.result as string);
          }
        }
      };
      reader.readAsText(file);
    }
  }, [file]);

  return (
    <div className="mx-12">
      <div className="flex h-[calc(100vh-240px)] rounded-lg overflow-hidden pt-5">
        <div className="w-[160px] min-w-[160px] h-full pt-2 border-r border-slate-200">
          {directoryTree.children.map((child) => (
            <TreeNode key={child.name} node={child} />
          ))}
        </div>
        <div className="flex-grow">
          <Editor language="yaml" value={code} />
        </div>
      </div>
      {/* <div className="flex"> */}
      <Button className="w-full mt-4" disabled>
        Save
      </Button>
      {/* </div> */}
    </div>
  );
};

export default EditorView;

interface FileNode {
  name: string;
  isDirectory: boolean;
}

interface FolderNode {
  name: string;
  isDirectory: boolean;
  children: TreeNodeType[];
}

type TreeNodeType = FileNode | FolderNode;

const TreeNode = ({ node }: { node: TreeNodeType }) => {
  const [isOpen, setIsOpen] = useState(false);

  const toggleOpen = (e: any) => {
    e.stopPropagation();
    setIsOpen((prev) => !prev);
  };

  return (
    <div
      onClick={
        node.isDirectory
          ? (e) => toggleOpen(e)
          : (e) => {
              e.stopPropagation();
              e.preventDefault();
            }
      }
    >
      <div className="cursor-pointer flex gap-1 items-center px-2 text-sm">
        <div className="w-4">
          {node.isDirectory && (
            <>
              {isOpen ? (
                <Icon size="16" iconName="RiArrowDownSLine" />
              ) : (
                <Icon size="16" iconName="RiArrowRightSLine" />
              )}
            </>
          )}
        </div>
        {node.name}
      </div>
      {isOpen &&
        node.isDirectory &&
        "children" in node && ( // <-- Type guard added here
          <div className="ml-4">
            {node.children.map((child) => (
              <TreeNode key={child.name} node={child} />
            ))}
          </div>
        )}
    </div>
  );
};
