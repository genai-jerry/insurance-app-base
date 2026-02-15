import {
  Box,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
  Alert,
} from '@mui/material';
import {
  People,
  ShoppingCart,
  Description,
  TrendingUp,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../api/admin';
import { LoadingSpinner } from '../../components/LoadingSpinner';

export const AdminDashboard = () => {
  const { data: stats, isLoading, error } = useQuery({
    queryKey: ['adminStats'],
    queryFn: adminApi.getDashboardStats,
    retry: false,
  });

  if (isLoading) {
    return <LoadingSpinner message="Loading dashboard..." />;
  }

  // Handle 404 or other errors gracefully with fallback content
  if (error || !stats) {
    const statCards = [
      {
        title: 'Total Leads',
        value: '-',
        icon: <People />,
        color: '#1976d2',
      },
      {
        title: 'New Leads',
        value: '-',
        icon: <TrendingUp />,
        color: '#388e3c',
      },
      {
        title: 'Calls Today',
        value: '-',
        icon: <ShoppingCart />,
        color: '#f57c00',
      },
      {
        title: 'Conversion Rate',
        value: '-',
        icon: <Description />,
        color: '#7b1fa2',
      },
    ];

    return (
      <Box>
        <Typography variant="h4" gutterBottom>
          Admin Dashboard
        </Typography>

        <Alert severity="info" sx={{ mb: 3 }}>
          Dashboard statistics are not yet available. The stats endpoint may not be configured.
        </Alert>

        <Grid container spacing={3}>
          {statCards.map((stat) => (
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
        </Grid>
      </Box>
    );
  }

  const statCards = [
    {
      title: 'Total Leads',
      value: stats.totalLeads || 0,
      icon: <People />,
      color: '#1976d2',
    },
    {
      title: 'New Leads',
      value: stats.newLeads || 0,
      icon: <TrendingUp />,
      color: '#388e3c',
    },
    {
      title: 'Calls Today',
      value: stats.callsToday || 0,
      icon: <ShoppingCart />,
      color: '#f57c00',
    },
    {
      title: 'Conversion Rate',
      value: `${(stats.conversionRate || 0).toFixed(1)}%`,
      icon: <Description />,
      color: '#7b1fa2',
    },
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Admin Dashboard
      </Typography>

      <Grid container spacing={3}>
        {statCards.map((stat) => (
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

        {/* Leads by Status */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Leads by Status
            </Typography>
            {stats.leadsByStatus && (
              <Box>
                {Object.entries(stats.leadsByStatus).map(([status, count]) => (
                  <Box
                    key={status}
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      py: 1,
                      borderBottom: 1,
                      borderColor: 'divider',
                    }}
                  >
                    <Typography>{status}</Typography>
                    <Typography fontWeight="bold">{count}</Typography>
                  </Box>
                ))}
              </Box>
            )}
          </Paper>
        </Grid>

        {/* System Overview */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              System Overview
            </Typography>
            <Box>
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  py: 1,
                  borderBottom: 1,
                  borderColor: 'divider',
                }}
              >
                <Typography>Pending Calls</Typography>
                <Typography fontWeight="bold">
                  {stats.callsPending || 0}
                </Typography>
              </Box>
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  py: 1,
                  borderBottom: 1,
                  borderColor: 'divider',
                }}
              >
                <Typography>Conversion Rate</Typography>
                <Typography fontWeight="bold">
                  {(stats.conversionRate || 0).toFixed(1)}%
                </Typography>
              </Box>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};
