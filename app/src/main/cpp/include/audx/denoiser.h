#ifndef AUDX_REALTIME_DENOISER_H
#define AUDX_REALTIME_DENOISER_H

#include "model_loader.h"
#include <stdbool.h>
#include <stdint.h>
#include <time.h>

/* Forward declarations for rnnoise types */
#ifdef __cplusplus
extern "C" {
#endif
typedef struct DenoiseState DenoiseState;
typedef struct RNNModel RNNModel;
#ifdef __cplusplus
}
#endif

#define FRAME_SIZE 480

#define REALTIME_DENOISER_SUCCESS 0
#define REALTIME_DENOISER_ERROR_INVALID -1
#define REALTIME_DENOISER_ERROR_MEMORY -2
#define REALTIME_DENOISER_ERROR_MODEL -3
#define REALTIME_DENOISER_ERROR_FORMAT -4

struct DenoiserConfig {
  int num_channels;              // 1 = mono, 2 = stereo
  enum ModelPreset model_preset; // Which model to use
  const char *model_path;        // Path to custom model, can be NULL
  float vad_threshold;           // 0.0-0.1 default 0.5
  bool enable_vad_output;        // Enable VAD in results
};

struct DenoiserResult {
  float vad_probability; // result of vad probability 0.0-1.0
  bool is_speech;        // above threshold?
  int samples_processed; // should be 480
};

/**
 * @brief Opaque denoiser context
 */
struct Denoiser {
  /* Configuration */
  int num_channels;
  float vad_threshold;
  bool enable_vad_output;

  /* RNNoise state (one per channel) */
  DenoiseState **denoisers;

  /* Loaded model (NULL for embedded) */
  RNNModel *model;

  /* Processing buffers (planar float) */
  float **processing_buffers; // [num_channels][480]

  /* Statistics */
  uint64_t frames_processed;
  uint64_t speech_frames;
  float total_vad_score;
  float min_vad_score;
  float max_vad_score;

  /* Timer */
  struct timespec start_time;
  struct timespec end_time;
  double total_processing_time;
  double last_frame_time;

  /* Last error */
  char error_buffer[256];
};

/**
 * @brief Create denoiser instance
 *
 * @param config    Configuration (must not be NULL)
 * @param denoiser  denoiser to identify
 *
 * @return 0 on success, negative error code on failure
 */
int denoiser_create(const struct DenoiserConfig *config,
                    struct Denoiser *denoiser);

/**
 * @brief Process audio samples
 *
 * REQUIREMENTS:
 * - input_pcm must be exactly 480 * num_channels sample
 * - Audio must be 48kHz, 16-bit signed PCM
 * - Stereo must be interleaved: [L, R, L, R, ...]
 *
 * @param denoiser      Denoiser context
 * @param input_pcm     Input samples
 * @param output_pcm    output samples (same size as input)
 * @param result        Result info (Optional, can be NULL)
 *
 * @return 0 on success, negative error code on failure
 */
int denoiser_process(struct Denoiser *denoiser, const int16_t *input_pcm,
                     int16_t *output_pcm, struct DenoiserResult *result);

/**
 * @brief Destroy denoiser instance
 *
 * @param denoiser  Denoiser to destroy
 */
void denoiser_destroy(struct Denoiser *denoiser);

/**
 * @brief Get last error message
 *
 * @param denoiser  Denoiser to track
 *
 * @return Error string or NULL
 */
const char *get_denoiser_error(struct Denoiser *denoiser);

/**
 * @brief get statistics
 *
 * @param denoiser  Denoiser to track
 *
 * @return Statistics string (do not free)
 */
const char *get_denoiser_stats(struct Denoiser *denoiser);

/**
 * @brief Get RNNoiser version
 *
 * @return version string
 */
const char *denoiser_version(void);

#endif // AUDX_REALTIME_DENOISER_H
