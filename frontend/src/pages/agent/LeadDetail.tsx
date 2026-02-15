import {
  Box,
  Paper,
  Typography,
  Grid,
  Chip,
  Button,
  TextField,
  MenuItem,
  Card,
  CardContent,
  List,
  ListItem,
  ListItemText,
  Divider,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { Phone, Email, Edit } from '@mui/icons-material';
import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { leadsApi } from '../../api/leads';
import { schedulerApi } from '../../api/scheduler';
import { voiceApi } from '../../api/voice';
import { emailApi } from '../../api/email';
import { prospectusApi } from '../../api/prospectus';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorAlert } from '../../components/ErrorAlert';
import { formatDate, formatDateTime, formatPhone } from '../../utils/formatters';
import { UpdateLeadRequest, LeadStatus, ScheduleCallRequest } from '../../types';

export const LeadDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [scheduleDialogOpen, setScheduleDialogOpen] = useState(false);

  const {
    register: registerEdit,
    handleSubmit: handleEditSubmit,
    reset: resetEdit,
    formState: { errors: editErrors },
  } = useForm<UpdateLeadRequest>();

  const {
    register: registerSchedule,
    handleSubmit: handleScheduleSubmit,
    reset: resetSchedule,
    formState: { errors: scheduleErrors },
  } = useForm<ScheduleCallRequest>();

  const { data: lead, isLoading, error } = useQuery({
    queryKey: ['lead', id],
    queryFn: () => leadsApi.getById(Number(id)),
    enabled: !!id,
  });

  const { data: voiceSessions } = useQuery({
    queryKey: ['voiceSessions', id],
    queryFn: () => voiceApi.getLeadSessions(Number(id)),
    enabled: !!id,
  });

  const { data: emails } = useQuery({
    queryKey: ['emails', id],
    queryFn: () => emailApi.getByLead(Number(id)),
    enabled: !!id,
  });

  const { data: prospectusRequests } = useQuery({
    queryKey: ['prospectus', id],
    queryFn: () => prospectusApi.getByLead(Number(id)),
    enabled: !!id,
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdateLeadRequest) =>
      leadsApi.update(Number(id), data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lead', id] });
      setEditDialogOpen(false);
      resetEdit();
    },
  });

  const scheduleMutation = useMutation({
    mutationFn: schedulerApi.schedule,
    onSuccess: () => {
      setScheduleDialogOpen(false);
      resetSchedule();
    },
  });

  const onEditSubmit = (data: UpdateLeadRequest) => {
    updateMutation.mutate(data);
  };

  const onScheduleSubmit = (data: any) => {
    scheduleMutation.mutate({
      leadId: Number(id),
      agentId: data.agentId,
      scheduledTime: data.scheduledTime,
      notes: data.notes,
    });
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading lead details..." />;
  }

  if (error || !lead) {
    return <ErrorAlert error={error as Error || 'Lead not found'} />;
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Lead Details</Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<Edit />}
            onClick={() => {
              resetEdit(lead);
              setEditDialogOpen(true);
            }}
            sx={{ mr: 1 }}
          >
            Edit
          </Button>
          <Button
            variant="contained"
            startIcon={<Phone />}
            onClick={() => setScheduleDialogOpen(true)}
          >
            Schedule Call
          </Button>
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Lead Information */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Contact Information
            </Typography>
            <List>
              <ListItem>
                <ListItemText primary="Name" secondary={lead.name} />
              </ListItem>
              <ListItem>
                <ListItemText primary="Email" secondary={lead.email} />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Phone"
                  secondary={formatPhone(lead.phone)}
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Status"
                  secondary={<Chip label={lead.status} color="primary" />}
                />
              </ListItem>
              <ListItem>
                <ListItemText primary="Source" secondary={lead.leadSource} />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Created"
                  secondary={formatDate(lead.createdAt)}
                />
              </ListItem>
              {lead.notes && (
                <ListItem>
                  <ListItemText primary="Notes" secondary={lead.notes} />
                </ListItem>
              )}
            </List>
          </Paper>
        </Grid>

        {/* Activity Timeline */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Recent Activity
            </Typography>

            {/* Voice Sessions */}
            {voiceSessions && voiceSessions.length > 0 && (
              <Box sx={{ mb: 3 }}>
                <Typography variant="subtitle2" color="primary" gutterBottom>
                  Voice Sessions ({voiceSessions.length})
                </Typography>
                {voiceSessions.slice(0, 3).map((session) => (
                  <Card key={session.id} sx={{ mb: 1 }}>
                    <CardContent>
                      <Typography variant="body2">
                        Duration: {Math.floor((session.durationSeconds || 0) / 60)} minutes
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {formatDateTime(session.createdAt)}
                      </Typography>
                      <Typography variant="caption" display="block">
                        Status: {session.status}
                      </Typography>
                    </CardContent>
                  </Card>
                ))}
              </Box>
            )}

            {/* Emails */}
            {emails && emails.length > 0 && (
              <Box sx={{ mb: 3 }}>
                <Typography variant="subtitle2" color="primary" gutterBottom>
                  Emails ({emails.length})
                </Typography>
                {emails.slice(0, 3).map((email) => (
                  <Card key={email.id} sx={{ mb: 1 }}>
                    <CardContent>
                      <Typography variant="body2">{email.subject}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {email.sentAt ? formatDateTime(email.sentAt) : 'Pending'}
                      </Typography>
                      <Chip
                        label={email.status}
                        size="small"
                        color={email.status === 'SENT' ? 'success' : 'default'}
                        sx={{ ml: 1 }}
                      />
                    </CardContent>
                  </Card>
                ))}
              </Box>
            )}

            {/* Prospectus */}
            {prospectusRequests && prospectusRequests.length > 0 && (
              <Box>
                <Typography variant="subtitle2" color="primary" gutterBottom>
                  Prospectus ({prospectusRequests.length})
                </Typography>
                {prospectusRequests.map((p) => (
                  <Card key={p.id} sx={{ mb: 1 }}>
                    <CardContent>
                      <Typography variant="body2">
                        Version: {p.version || 1}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {formatDateTime(p.createdAt)}
                      </Typography>
                      {p.pdfUrl && (
                        <Chip
                          label="PDF Ready"
                          size="small"
                          color="success"
                          sx={{ ml: 1 }}
                        />
                      )}
                    </CardContent>
                  </Card>
                ))}
              </Box>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Edit Dialog */}
      <Dialog
        open={editDialogOpen}
        onClose={() => setEditDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleEditSubmit(onEditSubmit)}>
          <DialogTitle>Edit Lead</DialogTitle>
          <DialogContent>
            <TextField
              fullWidth
              label="Name"
              margin="normal"
              defaultValue={lead.name}
              {...registerEdit('name')}
            />
            <TextField
              fullWidth
              label="Email"
              type="email"
              margin="normal"
              defaultValue={lead.email}
              {...registerEdit('email')}
            />
            <TextField
              fullWidth
              label="Phone"
              margin="normal"
              defaultValue={lead.phone}
              {...registerEdit('phone')}
            />
            <TextField
              fullWidth
              select
              label="Status"
              margin="normal"
              defaultValue={lead.status}
              {...registerEdit('status')}
            >
              <MenuItem value="NEW">New</MenuItem>
              <MenuItem value="CONTACTED">Contacted</MenuItem>
              <MenuItem value="QUALIFIED">Qualified</MenuItem>
              <MenuItem value="PROPOSAL_SENT">Proposal Sent</MenuItem>
              <MenuItem value="CONVERTED">Converted</MenuItem>
              <MenuItem value="LOST">Lost</MenuItem>
            </TextField>
            <TextField
              fullWidth
              label="Notes"
              multiline
              rows={3}
              margin="normal"
              defaultValue={lead.notes}
              {...registerEdit('notes')}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained">
              Save
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Schedule Call Dialog */}
      <Dialog
        open={scheduleDialogOpen}
        onClose={() => setScheduleDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleScheduleSubmit(onScheduleSubmit)}>
          <DialogTitle>Schedule Call</DialogTitle>
          <DialogContent>
            <TextField
              fullWidth
              label="Scheduled Time"
              type="datetime-local"
              margin="normal"
              InputLabelProps={{ shrink: true }}
              {...registerSchedule('scheduledTime', {
                required: 'Scheduled time is required',
              })}
              error={!!scheduleErrors.scheduledTime}
              helperText={scheduleErrors.scheduledTime?.message}
            />
            <TextField
              fullWidth
              label="Notes"
              multiline
              rows={3}
              margin="normal"
              {...registerSchedule('notes')}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setScheduleDialogOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="contained">
              Schedule
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};
