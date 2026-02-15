import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  Chip,
  Card,
  CardContent,
  Grid,
  Divider,
} from '@mui/material';
import { RecordVoiceOver, Psychology, Recommend } from '@mui/icons-material';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { voiceApi } from '../../api/voice';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorAlert } from '../../components/ErrorAlert';
import { formatDateTime } from '../../utils/formatters';

export const VoiceSessionViewer = () => {
  const { sessionId } = useParams<{ sessionId: string }>();

  const { data: session, isLoading, error } = useQuery({
    queryKey: ['voiceSession', sessionId],
    queryFn: () => voiceApi.getSession(Number(sessionId)),
    enabled: !!sessionId,
  });

  const { data: needs } = useQuery({
    queryKey: ['needs', sessionId],
    queryFn: () => voiceApi.getNeeds(Number(sessionId)),
    enabled: !!sessionId,
  });

  const { data: recommendations } = useQuery({
    queryKey: ['recommendations', sessionId],
    queryFn: () => voiceApi.getRecommendations(Number(sessionId)),
    enabled: !!sessionId,
  });

  if (isLoading) {
    return <LoadingSpinner message="Loading voice session..." />;
  }

  if (error || !session) {
    return <ErrorAlert error={error as Error || 'Session not found'} />;
  }

  const formatDurationFromSeconds = (seconds?: number): string => {
    if (!seconds) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Voice Session Details
      </Typography>

      <Grid container spacing={3}>
        {/* Session Info */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
              <RecordVoiceOver sx={{ mr: 1, color: 'primary.main' }} />
              <Typography variant="h6">Session Information</Typography>
            </Box>
            <List>
              <ListItem>
                <ListItemText
                  primary="Lead"
                  secondary={session.leadName}
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Duration"
                  secondary={formatDurationFromSeconds(session.durationSeconds)}
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Status"
                  secondary={
                    <Chip
                      label={session.status}
                      size="small"
                      color={session.status === 'COMPLETED' ? 'success' : 'default'}
                    />
                  }
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Created"
                  secondary={formatDateTime(session.createdAt)}
                />
              </ListItem>
            </List>
          </Paper>
        </Grid>

        {/* Transcript */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Transcript
            </Typography>
            <Box
              sx={{
                backgroundColor: 'background.default',
                p: 2,
                borderRadius: 1,
                maxHeight: 400,
                overflow: 'auto',
              }}
            >
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {session.transcriptText || 'No transcript available'}
              </Typography>
            </Box>
          </Paper>
        </Grid>

        {/* Extracted Needs */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
              <Psychology sx={{ mr: 1, color: 'secondary.main' }} />
              <Typography variant="h6">Extracted Needs</Typography>
            </Box>
            {needs && Object.keys(needs).length > 0 ? (
              <Box sx={{ backgroundColor: 'background.default', p: 2, borderRadius: 1 }}>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {JSON.stringify(needs, null, 2)}
                </Typography>
              </Box>
            ) : (
              <Typography color="text.secondary">No needs extracted</Typography>
            )}
          </Paper>
        </Grid>

        {/* Product Recommendations */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
              <Recommend sx={{ mr: 1, color: 'success.main' }} />
              <Typography variant="h6">Product Recommendations</Typography>
            </Box>
            {recommendations && Object.keys(recommendations).length > 0 ? (
              <Box sx={{ backgroundColor: 'background.default', p: 2, borderRadius: 1 }}>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {JSON.stringify(recommendations, null, 2)}
                </Typography>
              </Box>
            ) : (
              <Typography color="text.secondary">
                No recommendations generated
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};
