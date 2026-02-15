import {
  Box,
  Paper,
  Typography,
  Button,
  List,
  ListItem,
  ListItemText,
  Grid,
} from '@mui/material';
import { Download, Visibility } from '@mui/icons-material';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { prospectusApi } from '../../api/prospectus';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ErrorAlert } from '../../components/ErrorAlert';
import { formatDateTime } from '../../utils/formatters';

export const ProspectusPreview = () => {
  const { requestId } = useParams<{ requestId: string }>();

  const { data: prospectus, isLoading, error } = useQuery({
    queryKey: ['prospectus', requestId],
    queryFn: () => prospectusApi.getById(Number(requestId)),
    enabled: !!requestId,
  });

  const handleDownload = async () => {
    if (!requestId) return;
    try {
      const blob = await prospectusApi.download(Number(requestId));
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `prospectus-${requestId}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Download failed:', err);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading prospectus..." />;
  }

  if (error || !prospectus) {
    return <ErrorAlert error={error as Error || 'Prospectus not found'} />;
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Prospectus Preview</Typography>
        <Box>
          {prospectus.pdfUrl && (
            <Button
              variant="contained"
              startIcon={<Download />}
              onClick={handleDownload}
            >
              Download PDF
            </Button>
          )}
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Prospectus Details */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Prospectus Details
            </Typography>
            <List>
              <ListItem>
                <ListItemText
                  primary="Lead"
                  secondary={prospectus.leadName}
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Agent"
                  secondary={prospectus.agentName}
                />
              </ListItem>
              {prospectus.version && (
                <ListItem>
                  <ListItemText
                    primary="Version"
                    secondary={prospectus.version}
                  />
                </ListItem>
              )}
              <ListItem>
                <ListItemText
                  primary="Created"
                  secondary={formatDateTime(prospectus.createdAt)}
                />
              </ListItem>
            </List>
          </Paper>
        </Grid>

        {/* Preview/Download */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Document Preview
            </Typography>
            {prospectus.htmlContent ? (
              <Box
                sx={{
                  backgroundColor: 'background.default',
                  p: 2,
                  borderRadius: 1,
                  maxHeight: 600,
                  overflow: 'auto',
                }}
                dangerouslySetInnerHTML={{ __html: prospectus.htmlContent }}
              />
            ) : prospectus.pdfUrl ? (
              <Box sx={{ textAlign: 'center', p: 4 }}>
                <Typography variant="body1" gutterBottom>
                  Prospectus has been generated successfully!
                </Typography>
                <Button
                  variant="contained"
                  startIcon={<Visibility />}
                  onClick={handleDownload}
                  sx={{ mt: 2 }}
                >
                  View/Download PDF
                </Button>
              </Box>
            ) : (
              <Box sx={{ textAlign: 'center', p: 4 }}>
                <Typography variant="body1" color="text.secondary">
                  No preview available
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};
