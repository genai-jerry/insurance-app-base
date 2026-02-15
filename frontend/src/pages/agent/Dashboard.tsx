import {
  Box,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
  List,
  ListItem,
  ListItemText,
  Chip,
  Button,
} from '@mui/material';
import {
  Phone,
  People,
  TrendingUp,
  CheckCircle,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { schedulerApi } from '../../api/scheduler';
import { leadsApi } from '../../api/leads';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorAlert } from '../../components/ErrorAlert';
import { formatDateTime, formatDate } from '../../utils/formatters';

export const Dashboard = () => {
  const navigate = useNavigate();

  const { data: todayTasks, isLoading: tasksLoading, error: tasksError } = useQuery({
    queryKey: ['todayTasks'],
    queryFn: schedulerApi.getToday,
  });

  const { data: myLeads, isLoading: leadsLoading, error: leadsError } = useQuery({
    queryKey: ['myLeads'],
    queryFn: () => leadsApi.getMyLeads({ size: 10 }),
  });

  if (tasksLoading || leadsLoading) {
    return <LoadingSpinner message="Loading dashboard..." />;
  }

  const error = tasksError || leadsError;
  if (error) {
    return <ErrorAlert error={error as Error} />;
  }

  const pendingTasks = todayTasks?.filter((t) => t.status === 'PENDING') || [];
  const completedTasks = todayTasks?.filter((t) => t.status === 'DONE') || [];
  const newLeads = myLeads?.content.filter((l) => l.status === 'NEW') || [];

  const stats = [
    {
      title: 'Calls Today',
      value: todayTasks?.length || 0,
      icon: <Phone />,
      color: '#1976d2',
    },
    {
      title: 'Pending Calls',
      value: pendingTasks.length,
      icon: <CheckCircle />,
      color: '#f57c00',
    },
    {
      title: 'My Leads',
      value: myLeads?.totalElements || 0,
      icon: <People />,
      color: '#388e3c',
    },
    {
      title: 'New Leads',
      value: newLeads.length,
      icon: <TrendingUp />,
      color: '#7b1fa2',
    },
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>

      <Grid container spacing={3}>
        {/* Stats Cards */}
        {stats.map((stat) => (
          <Grid item xs={12} sm={6} md={3} key={stat.title}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <Box
                    sx={{
                      backgroundColor: stat.color,
                      color: 'white',
                      p: 1,
                      borderRadius: 1,
                      mr: 2,
                    }}
                  >
                    {stat.icon}
                  </Box>
                  <Typography variant="h4">{stat.value}</Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  {stat.title}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}

        {/* Today's Call Queue */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6">Today's Call Queue</Typography>
              <Button
                size="small"
                onClick={() => navigate('/agent/calendar')}
              >
                View All
              </Button>
            </Box>
            {pendingTasks.length === 0 ? (
              <Typography color="text.secondary">
                No pending calls for today
              </Typography>
            ) : (
              <List>
                {pendingTasks.slice(0, 5).map((task) => (
                  <ListItem
                    key={task.id}
                    sx={{
                      borderLeft: 3,
                      borderColor: 'primary.main',
                      mb: 1,
                      bgcolor: 'background.default',
                    }}
                  >
                    <ListItemText
                      primary={task.leadName}
                      secondary={
                        <>
                          <Typography variant="caption" display="block">
                            {formatDateTime(task.scheduledTime)}
                          </Typography>
                          {task.leadPhone && (
                            <Typography variant="caption" display="block">
                              {task.leadPhone}
                            </Typography>
                          )}
                        </>
                      }
                    />
                    <Chip label={task.status} size="small" color="warning" />
                  </ListItem>
                ))}
              </List>
            )}
          </Paper>
        </Grid>

        {/* Recent Leads */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6">Recent Leads</Typography>
              <Button
                size="small"
                onClick={() => navigate('/agent/leads')}
              >
                View All
              </Button>
            </Box>
            {myLeads?.content.length === 0 ? (
              <Typography color="text.secondary">
                No leads assigned yet
              </Typography>
            ) : (
              <List>
                {myLeads?.content.slice(0, 5).map((lead) => (
                  <ListItem
                    key={lead.id}
                    sx={{
                      mb: 1,
                      bgcolor: 'background.default',
                      cursor: 'pointer',
                      '&:hover': { bgcolor: 'action.hover' },
                    }}
                    onClick={() => navigate(`/agent/leads/${lead.id}`)}
                  >
                    <ListItemText
                      primary={lead.name}
                      secondary={
                        <>
                          <Typography variant="caption" display="block">
                            {lead.email}
                          </Typography>
                          <Typography variant="caption" display="block">
                            Created: {formatDate(lead.createdAt)}
                          </Typography>
                        </>
                      }
                    />
                    <Chip
                      label={lead.status}
                      size="small"
                      color={lead.status === 'NEW' ? 'success' : 'default'}
                    />
                  </ListItem>
                ))}
              </List>
            )}
          </Paper>
        </Grid>

        {/* Completed Calls Today */}
        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Completed Calls Today ({completedTasks.length})
            </Typography>
            {completedTasks.length === 0 ? (
              <Typography color="text.secondary">
                No completed calls yet
              </Typography>
            ) : (
              <List>
                {completedTasks.map((task) => (
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
                          <Typography variant="caption" display="block">
                            Completed: {formatDateTime(task.completedAt!)}
                          </Typography>
                          {task.notes && (
                            <Typography variant="caption" display="block">
                              Notes: {task.notes}
                            </Typography>
                          )}
                        </>
                      }
                    />
                    <Chip label="Completed" size="small" color="success" />
                  </ListItem>
                ))}
              </List>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};
