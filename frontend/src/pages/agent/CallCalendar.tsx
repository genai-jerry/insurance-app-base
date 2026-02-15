import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material';
import { Phone, CheckCircle } from '@mui/icons-material';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { schedulerApi } from '../../api/scheduler';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorAlert } from '../../components/ErrorAlert';
import { formatDateTime } from '../../utils/formatters';
import { CallTask } from '../../types';

export const CallCalendar = () => {
  const queryClient = useQueryClient();
  const [completeDialogOpen, setCompleteDialogOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState<CallTask | null>(null);

  const { register, handleSubmit, reset } = useForm<{ notes: string }>();

  const { data: tasks, isLoading, error } = useQuery({
    queryKey: ['myTasks'],
    queryFn: () => schedulerApi.getAll(),
  });

  const completeMutation = useMutation({
    mutationFn: ({ id, notes }: { id: number; notes?: string }) =>
      schedulerApi.complete(id, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myTasks'] });
      setCompleteDialogOpen(false);
      setSelectedTask(null);
      reset();
    },
  });

  // Cancel endpoint not available in backend

  const onCompleteSubmit = (data: { notes: string }) => {
    if (selectedTask) {
      completeMutation.mutate({ id: selectedTask.id, notes: data.notes });
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading call calendar..." />;
  }

  if (error) {
    return <ErrorAlert error={error as Error} />;
  }

  const pendingTasks = tasks?.filter((t: CallTask) => t.status === 'PENDING') || [];
  const completedTasks = tasks?.filter((t: CallTask) => t.status === 'DONE') || [];
  const cancelledTasks = tasks?.filter((t: CallTask) => t.status === 'CANCELLED') || [];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Call Calendar
      </Typography>

      <Box sx={{ display: 'flex', gap: 3, flexDirection: 'column' }}>
        {/* Pending Calls */}
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom color="warning.main">
            Pending Calls ({pendingTasks.length})
          </Typography>
          {pendingTasks.length === 0 ? (
            <Typography color="text.secondary">No pending calls</Typography>
          ) : (
            <List>
              {pendingTasks.map((task: CallTask) => (
                <ListItem
                  key={task.id}
                  sx={{
                    borderLeft: 3,
                    borderColor: 'warning.main',
                    mb: 1,
                    bgcolor: 'background.default',
                  }}
                  secondaryAction={
                    <Box>
                      <Button
                        size="small"
                        variant="contained"
                        color="success"
                        startIcon={<CheckCircle />}
                        onClick={() => {
                          setSelectedTask(task);
                          setCompleteDialogOpen(true);
                        }}
                        sx={{ mr: 1 }}
                      >
                        Complete
                      </Button>
                      {/* Cancel not available in backend */}
                    </Box>
                  }
                >
                  <ListItemText
                    primary={task.leadName}
                    secondary={
                      <>
                        <Typography variant="body2" display="block">
                          Scheduled: {formatDateTime(task.scheduledTime)}
                        </Typography>
                        {task.leadPhone && (
                          <Typography variant="body2" display="block">
                            Phone: {task.leadPhone}
                          </Typography>
                        )}
                        {task.notes && (
                          <Typography variant="body2" display="block">
                            Notes: {task.notes}
                          </Typography>
                        )}
                      </>
                    }
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Paper>

        {/* Completed Calls */}
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom color="success.main">
            Completed Calls ({completedTasks.length})
          </Typography>
          {completedTasks.length === 0 ? (
            <Typography color="text.secondary">No completed calls</Typography>
          ) : (
            <List>
              {completedTasks.map((task: CallTask) => (
                <ListItem
                  key={task.id}
                  sx={{
                    borderLeft: 3,
                    borderColor: 'success.main',
                    mb: 1,
                    bgcolor: 'background.default',
                  }}
                >
                  <ListItemText
                    primary={task.leadName}
                    secondary={
                      <>
                        <Typography variant="body2" display="block">
                          Completed: {formatDateTime(task.completedAt!)}
                        </Typography>
                        {task.notes && (
                          <Typography variant="body2" display="block">
                            Notes: {task.notes}
                          </Typography>
                        )}
                      </>
                    }
                  />
                  <Chip label="Completed" color="success" size="small" />
                </ListItem>
              ))}
            </List>
          )}
        </Paper>

        {/* Cancelled Calls */}
        {cancelledTasks.length > 0 && (
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom color="error.main">
              Cancelled Calls ({cancelledTasks.length})
            </Typography>
            <List>
              {cancelledTasks.map((task: CallTask) => (
                <ListItem
                  key={task.id}
                  sx={{
                    borderLeft: 3,
                    borderColor: 'error.main',
                    mb: 1,
                    bgcolor: 'background.default',
                  }}
                >
                  <ListItemText
                    primary={task.leadName}
                    secondary={formatDateTime(task.scheduledTime)}
                  />
                  <Chip label="Cancelled" color="error" size="small" />
                </ListItem>
              ))}
            </List>
          </Paper>
        )}
      </Box>

      {/* Complete Call Dialog */}
      <Dialog
        open={completeDialogOpen}
        onClose={() => setCompleteDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleSubmit(onCompleteSubmit)}>
          <DialogTitle>Complete Call</DialogTitle>
          <DialogContent>
            <Typography variant="body2" gutterBottom>
              Lead: {selectedTask?.leadName}
            </Typography>
            <TextField
              fullWidth
              label="Call Notes"
              multiline
              rows={4}
              margin="normal"
              {...register('notes')}
              placeholder="Enter call summary and outcomes..."
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCompleteDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              color="success"
              disabled={completeMutation.isPending}
            >
              Complete Call
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};
