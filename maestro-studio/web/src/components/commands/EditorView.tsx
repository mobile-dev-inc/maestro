import Editor from "@monaco-editor/react";
import { useEffect, useState } from "react";
import { Icon } from "../design-system/icon";
import { Button } from "../design-system/button";
import { API } from "../../api/api";
import { node } from "prop-types";

interface FileNode {
  name: string;
  absolutePath: string;
  isDirectory: boolean;
}

interface FolderNode {
  name: string;
  isDirectory: boolean;
  absolutePath: string;
  children: TreeNodeType[];
}

type TreeNodeType = FileNode | FolderNode;

const EditorView = ({ directory }: { directory: TreeNodeType[] | null }) => {
  const [code, setCode] = useState("");
  const [editorCode, setEditorCode] = useState("");
  const [selectedFile, setSelectedFile] = useState<any>(null);

  const handleFileChange = async (e: any, node: TreeNodeType) => {
    e.stopPropagation();
    try {
      const response: { content: string } = (await API.readFile(
        node.absolutePath
      )) as { content: string };
      setSelectedFile(node);
      setCode(response.content);
      setEditorCode(response.content);
    } catch (error) {
      setSelectedFile(null);
      setCode("");
      setEditorCode("");
    }
  };

  const saveFile = async () => {
    try {
      const response = await API.saveFile(
        editorCode,
        selectedFile.absolutePath
      );
      setCode(editorCode);
    } catch (error) {}
  };

  const getFileExtension = (filename: string) =>
    filename.includes(".") ? filename.split(".").pop() : undefined;

  return (
    <div className="mx-12">
      <div className="flex h-[calc(100vh-240px)] rounded-lg overflow-hidden pt-5">
        <div className="w-[160px] min-w-[160px] h-full pt-2 border-r border-slate-200">
          {directory?.map((child) => (
            <TreeNode
              selectedFile={selectedFile}
              key={child.name}
              node={child}
              handleFileChange={handleFileChange}
            />
          ))}
        </div>
        <div className="flex-grow">
          <Editor
            language={getFileExtension(node.name)}
            value={editorCode}
            onChange={(val) => setEditorCode(val || "")}
          />
        </div>
      </div>
      <Button
        onClick={saveFile}
        className="w-full mt-4"
        disabled={editorCode === code}
      >
        Save
      </Button>
    </div>
  );
};

export default EditorView;

const TreeNode = ({
  node,
  selectedFile,
  handleFileChange,
}: {
  node: TreeNodeType;
  selectedFile: TreeNodeType;
  handleFileChange: (e: any, node: TreeNodeType) => void;
}) => {
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
          : (e) => handleFileChange(e, node)
      }
      className={
        !node.isDirectory && selectedFile === node
          ? `bg-purple-100 rounded-lg`
          : ""
      }
    >
      <div className="cursor-pointer flex gap-1 items-center px-2 text-sm">
        <div className="w-4 min-w-4">
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
        <p className="truncate">{node.name}</p>
      </div>
      {isOpen && node.isDirectory && "children" in node && (
        <div className="ml-4">
          {node.children.map((child) => (
            <TreeNode
              selectedFile={selectedFile}
              key={child.name}
              node={child}
              handleFileChange={handleFileChange}
            />
          ))}
        </div>
      )}
    </div>
  );
};
