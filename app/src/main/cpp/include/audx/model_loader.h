#ifndef AUDX_MODEL_LOADER_H
#define AUDX_MODEL_LOADER_H

#include <stdbool.h>

/**
 * @file model_loader.h
 * @brief RNNoise model management utilities
 *
 * Provides functions for:
 * - Mapping model presets to file paths
 * - Resolving model locations (bundled, user, custom)
 * - Validating model files
 * - Listing available models
 */

/**
 * @brief RNNoise model presets
 *
 * RNNoise supports the embedded model (built into librnnoise) or
 * custom user-trained models in binary blob format.
 *
 * Custom models must be generated using dump_weights_blob.c from
 * the rnnoise training repository. See assets/models/rnnoise/README.md
 * for training instructions.
 */
enum ModelPreset {
  /** Embedded RNNoise model (built into librnnoise) - Default */
  MODEL_EMBEDDED,
  /** Custom user-provided model in binary blob format */
  MODEL_CUSTOM
};

/**
 * @brief RNNoise model information
 *
 * Describes a pre-trained RNNoise model including its
 * preset identifier, name, description, and file path.
 */
struct ModelInfo {
  /** Model preset enum value */
  enum ModelPreset preset;

  /** Short model identifier (e.g., "sh", "bd") */
  const char *name;

  /** Human-readable description */
  const char *description;

  /** Default file path (relative to assets dir or absolute) */
  const char *default_path;
};

/**
 * @brief Get model file path for preset
 *
 * For MODEL_EMBEDDED, returns NULL (use rnnoise_create(NULL)).
 * For MODEL_CUSTOM, this function should not be used - pass
 * the custom path directly to the denoiser.
 *
 * @param preset Model preset enum
 * @return NULL for EMBEDDED, unused for CUSTOM
 * @note This function is deprecated and will be removed
 */
const char *get_model_path_for_preset(enum ModelPreset preset);

/**
 * @brief Get model information for preset
 *
 * Returns detailed information about a model preset including
 * name, description, and default path.
 *
 * @param preset Model preset enum
 * @return Pointer to model info structure, or NULL if invalid preset
 * @note Returned pointer points to static data, do not free
 */
const struct ModelInfo *get_model_info(enum ModelPreset preset);

/**
 * @brief Validate model file
 *
 * Checks if a model file exists and is readable.
 * Does NOT validate the model format (RNNoise does that on load).
 *
 * @param path Path to model file
 * @return true if file exists and is readable, false otherwise
 */
bool validate_model_file(const char *path);

/**
 * @brief Get array of all available model presets
 *
 * Returns an array of ModelInfo structures describing
 * all built-in model presets (excluding MODEL_CUSTOM).
 *
 * @param count Output: Number of models in array
 * @return Pointer to static array of model info structures
 * @note Do not free the returned pointer
 */
const struct ModelInfo *get_all_models(int *count);

/**
 * @brief List all available models to stdout
 *
 * Prints a formatted list of all available models with their
 * descriptions and availability status.
 */
void list_available_models(void);

/**
 * @brief Get model preset from name string
 *
 * Converts a model name string (e.g., "sh", "bd") to the
 * corresponding preset enum value.
 *
 * @param name Model name string (case-insensitive)
 * @return Model preset enum, or MODEL_CUSTOM if not found
 */
enum ModelPreset get_preset_from_name(const char *name);

#endif // AUDX_MODEL_LOADER_H
