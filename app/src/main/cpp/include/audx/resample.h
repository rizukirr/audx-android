#ifndef AUDX_RESAMPLER_H
#define AUDX_RESAMPLER_H

#include "audx/common.h"
#include <speex/speex_resampler.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Maximum allowed resampler quality level.
 *
 * Higher quality increases CPU usage and improves accuracy.
 */
#define AUDX_RESAMPLER_QUALITY_MAX 10

/**
 * @brief Minimum allowed resampler quality level.
 *
 * Quality 0 is the fastest but offers lowest audio accuracy.
 */
#define AUDX_RESAMPLER_QUALITY_MIN 0

/**
 * @brief Default resampler quality level.
 *
 * Balanced between performance and quality.
 */
#define AUDX_RESAMPLER_QUALITY_DEFAULT 4

/**
 * @brief Recommended resampler quality for VoIP or low-latency scenarios.
 *
 * Provides good clarity with low computational cost.
 */
#define AUDX_RESAMPLER_QUALITY_VOIP 3

/**
 * @brief Internal state for a resampler instance.
 *
 * Stores configuration and input/output buffers for a single
 * resampling operation. This struct is used internally and is
 * typically encapsulated by an opaque `AudxResampler` handle.
 */
struct AudxResamplerState {
  /**
   * Number of audio channels (e.g., 1 = mono, 2 = stereo).
   */
  audx_uint32_t nb_channels;

  /**
   * Original input sample rate in Hz.
   */
  audx_uint32_t input_sample_rate;

  /**
   * Desired output sample rate in Hz.
   */
  audx_uint32_t output_sample_rate;

  /**
   * Resampling quality (0–10).
   * Higher values yield better audio but use more CPU.
   */
  int quality;

  /**
   * Pointer to the input audio samples (int16 PCM).
   */
  const audx_int16_t *input_int16;

  /**
   * Number of input samples available.
   */
  audx_uint32_t input_len;

  /**
   * Pointer to the output audio buffer.
   */
  audx_int16_t *output_int16;

  /**
   * Size of the output buffer (in samples).
   */
  audx_uint32_t output_len;

  /**
   * Actual number of samples written to the output buffer.
   */
  audx_uint32_t *actual_output_len;
};

/**
 * @brief Opaque handle to a resampler instance.
 *
 * Users interact only with this pointer; the underlying state is hidden.
 */
typedef void *AudxResampler;

/**
 * @brief Create a persistent resampler instance.
 *
 * Initializes a resampler configured for transforming audio from
 * `input_sample_rate` to `output_sample_rate` using a given quality level.
 *
 * @param nb_channels         Number of audio channels (1 = mono, 2 = stereo).
 * @param input_sample_rate   Input sampling rate in Hz.
 * @param output_sample_rate  Output sampling rate in Hz.
 * @param quality             Resampling quality (0–10, where 10 is best).
 * @param err                 Optional pointer to receive an error code.
 *                            Set to 0 on success.
 *
 * @return A valid `AudxResampler` handle on success, or NULL on failure.
 */
AudxResampler audx_resample_create(audx_uint32_t nb_channels,
                                   audx_uint32_t input_sample_rate,
                                   audx_uint32_t output_sample_rate,
                                   int quality, int *err);

/**
 * @brief Process audio samples through the resampler.
 *
 * Converts audio from the input sample rate to the configured output rate.
 * Both `input_len` and `output_len` are updated to reflect consumed and
 * produced samples.
 *
 * @param resampler   Resampler handle created via audx_resample_create().
 * @param input       Pointer to input PCM samples (int16).
 * @param input_len   On input: number of available samples.
 *                    On output: number of samples actually consumed.
 * @param output      Pointer to output buffer for resampled audio.
 * @param output_len  On input: size of the output buffer.
 *                    On output: number of samples written.
 *
 * @return AUDX_RESAMPLER_SUCCESS on success, or negative error code.
 */
int audx_resample_process(AudxResampler resampler, const audx_int16_t *input,
                          audx_uint32_t *input_len, audx_int16_t *output,
                          audx_uint32_t *output_len);

/**
 * @brief Destroy a resampler instance.
 *
 * Frees all memory and internal resources used by the resampler.
 *
 * @param resampler   Handle returned from audx_resample_create().
 */
void audx_resample_destroy(AudxResampler resampler);

#ifdef __cplusplus
}
#endif

#endif // AUDX_RESAMPLER_H
