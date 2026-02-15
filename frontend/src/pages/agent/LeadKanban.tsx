import {
  Box,
  Paper,
  Typography,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
} from '@mui/material';
import { Phone, Email } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { leadsApi } from '../../api/leads';
import { Lead, LeadStatus } from '../../types';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorAlert } from '../../components/ErrorAlert';
import { formatPhone } from '../../utils/formatters';

const columns: { status: LeadStatus; title: string; color: string }[] = [
  { status: 'NEW', title: 'New', color: '#4caf50' },
  { status: 'CONTACTED', title: 'Contacted', color: '#2196f3' },
  { status: 'QUALIFIED', title: 'Qualified', color: '#9c27b0' },
  { status: 'PROPOSAL_SENT', title: 'Proposal Sent', color: '#ff9800' },
  { status: 'CONVERTED', title: 'Converted', color: '#4caf50' },
  { status: 'LOST', title: 'Lost', color: '#f44336' },
];

export const LeadKanban = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: leads, isLoading, error } = useQuery({
    queryKey: ['allLeads'],
    queryFn: () => leadsApi.getAll({ size: 100 }),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: LeadStatus }) =>
      leadsApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['allLeads'] });
    },
  });

  if (isLoading) {
    return <LoadingSpinner message="Loading kanban board..." />;
  }

  if (error) {
    return <ErrorAlert error={error as Error} />;
  }

  const getLeadsByStatus = (status: LeadStatus): Lead[] => {
    return leads?.content.filter((lead) => lead.status === status) || [];
  };

  const handleStatusChange = (leadId: number, newStatus: LeadStatus) => {
    updateStatusMutation.mutate({ id: leadId, status: newStatus });
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Lead Kanban Board
      </Typography>

      <Box sx={{ overflowX: 'auto' }}>
        <Box sx={{ display: 'flex', gap: 2, minWidth: 'min-content', pb: 2 }}>
          {columns.map((column) => {
            const columnLeads = getLeadsByStatus(column.status);
            return (
              <Paper
                key={column.status}
                sx={{
                  minWidth: 300,
                  maxWidth: 300,
                  backgroundColor: 'background.default',
                }}
              >
                <Box
                  sx={{
                    p: 2,
                    backgroundColor: column.color,
                    color: 'white',
                  }}
                >
                  <Typography variant="h6">
                    {column.title} ({columnLeads.length})
                  </Typography>
                </Box>
                <Box sx={{ p: 2, maxHeight: 'calc(100vh - 250px)', overflow: 'auto' }}>
                  {columnLeads.map((lead) => (
                    <Card key={lead.id} sx={{ mb: 2 }}>
                      <CardContent>
                        <Typography variant="h6" gutterBottom>
                          {lead.name}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {lead.email}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {formatPhone(lead.phone)}
                        </Typography>
                        <Chip
                          label={lead.leadSource}
                          size="small"
                          sx={{ mt: 1 }}
                        />
                      </CardContent>
                      <CardActions>
                        <Button
                          size="small"
                          onClick={() => navigate(`/agent/leads/${lead.id}`)}
                        >
                          View
                        </Button>
                        <Button size="small" startIcon={<Phone />}>
                          Call
                        </Button>
                      </CardActions>
                    </Card>
                  ))}
                  {columnLeads.length === 0 && (
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      align="center"
                    >
                      No leads
                    </Typography>
                  )}
                </Box>
              </Paper>
            );
          })}
        </Box>
      </Box>
    </Box>
  );
};
