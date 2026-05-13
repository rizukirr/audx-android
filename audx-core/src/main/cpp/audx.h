#ifndef AUDX_H
#define AUDX_H

#ifdef __cplusplus
extern "C" {
#endif

// RNNoise requires 48kHz input and output
#define FRAME_RATE 48000
#define FRAME_SIZE 480

typedef struct AudxState AudxState;

unsigned int audx_calculate_frame_sample(unsigned int sample_rate);

AudxState *audx_create(const char *model_path, unsigned int in_rate,
                       int resample_quality);

float audx_process(AudxState *state, float *in, float *out);

float audx_process_int(AudxState *state, short *in, short *out);

void audx_destroy(AudxState *state);

#ifdef __cplusplus
}
#endif

#endif // AUDX_H
