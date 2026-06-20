import React, { useState } from "react";
import { Eye, Edit2, Trash2, Check, X, Download, FileOutput, Search, RefreshCw, ArrowLeft, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

export interface ActionBarProps {
  onView?: () => void;
  onEdit?: () => void;
  onDelete?: () => Promise<void> | void;
  onApprove?: () => Promise<void> | void;
  onReject?: () => Promise<void> | void;
  onDownload?: () => void;
  onExport?: () => void;
  onSearch?: () => void;
  onRefresh?: () => Promise<void> | void;
  onBack?: () => void;
  
  deleteMessage?: string;
  rejectMessage?: string;
  className?: string;
}

export function ActionBar({
  onView,
  onEdit,
  onDelete,
  onApprove,
  onReject,
  onDownload,
  onExport,
  onSearch,
  onRefresh,
  onBack,
  deleteMessage = "Are you sure you want to delete this record? This action cannot be undone.",
  rejectMessage = "Are you sure you want to reject this request?",
  className = "flex flex-wrap items-center gap-2",
}: ActionBarProps) {
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [loadingAction, setLoadingAction] = useState<string | null>(null);

  const handleAsyncAction = async (actionName: string, actionFn: () => Promise<void> | void) => {
    setLoadingAction(actionName);
    try {
      await actionFn();
    } finally {
      setLoadingAction(null);
    }
  };

  return (
    <div className={className}>
      {onBack && (
        <Button variant="outline" size="sm" onClick={() => handleAsyncAction("back", onBack)} disabled={!!loadingAction}>
          <ArrowLeft className="h-4 w-4 mr-2" /> Back
        </Button>
      )}
      {onSearch && (
        <Button variant="outline" size="sm" onClick={() => handleAsyncAction("search", onSearch)} disabled={!!loadingAction}>
          <Search className="h-4 w-4 mr-2" /> Search
        </Button>
      )}
      {onRefresh && (
        <Button variant="outline" size="sm" onClick={() => handleAsyncAction("refresh", onRefresh)} disabled={!!loadingAction}>
          {loadingAction === "refresh" ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <RefreshCw className="h-4 w-4 mr-2" />} 
          Refresh
        </Button>
      )}
      {onView && (
        <Button variant="secondary" size="sm" onClick={() => handleAsyncAction("view", onView)} disabled={!!loadingAction}>
          <Eye className="h-4 w-4 mr-2" /> View
        </Button>
      )}
      {onEdit && (
        <Button variant="default" size="sm" onClick={() => handleAsyncAction("edit", onEdit)} disabled={!!loadingAction}>
          <Edit2 className="h-4 w-4 mr-2" /> Edit
        </Button>
      )}
      {onDownload && (
        <Button variant="outline" size="sm" onClick={() => handleAsyncAction("download", onDownload)} disabled={!!loadingAction}>
          <Download className="h-4 w-4 mr-2" /> Download
        </Button>
      )}
      {onExport && (
        <Button variant="outline" size="sm" onClick={() => handleAsyncAction("export", onExport)} disabled={!!loadingAction}>
          <FileOutput className="h-4 w-4 mr-2" /> Export
        </Button>
      )}
      {onApprove && (
        <Button className="bg-success text-success-foreground hover:bg-success/90" size="sm" onClick={() => handleAsyncAction("approve", onApprove)} disabled={!!loadingAction}>
          {loadingAction === "approve" ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Check className="h-4 w-4 mr-2" />} 
          Approve
        </Button>
      )}
      {onReject && (
        <Button variant="destructive" size="sm" onClick={() => setRejectOpen(true)} disabled={!!loadingAction}>
          <X className="h-4 w-4 mr-2" /> Reject
        </Button>
      )}
      {onDelete && (
        <Button variant="destructive" size="sm" onClick={() => setDeleteOpen(true)} disabled={!!loadingAction}>
          <Trash2 className="h-4 w-4 mr-2" /> Delete
        </Button>
      )}

      <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Confirm Deletion</AlertDialogTitle>
            <AlertDialogDescription>{deleteMessage}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={(e) => {
                e.preventDefault();
                setDeleteOpen(false);
                if (onDelete) handleAsyncAction("delete", onDelete);
              }}
            >
              {loadingAction === "delete" ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Trash2 className="h-4 w-4 mr-2" />}
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={rejectOpen} onOpenChange={setRejectOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Confirm Rejection</AlertDialogTitle>
            <AlertDialogDescription>{rejectMessage}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={(e) => {
                e.preventDefault();
                setRejectOpen(false);
                if (onReject) handleAsyncAction("reject", onReject);
              }}
            >
              {loadingAction === "reject" ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <X className="h-4 w-4 mr-2" />}
              Reject
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
