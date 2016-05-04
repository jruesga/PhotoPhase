/*
 * Copyright (C) 2016 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

attribute vec4 aPosition;
attribute vec2 aTextureCoord;

varying vec2 v_texcoord;
varying vec2 v_blurTexCoords[14];

uniform float strength;

void main()
{
    gl_Position = aPosition;
    v_texcoord = aTextureCoord;
    v_blurTexCoords[ 0] = v_texcoord + vec2(-0.028 * strength, 0.0);
    v_blurTexCoords[ 1] = v_texcoord + vec2(-0.024 * strength, 0.0);
    v_blurTexCoords[ 2] = v_texcoord + vec2(-0.020 * strength, 0.0);
    v_blurTexCoords[ 3] = v_texcoord + vec2(-0.016 * strength, 0.0);
    v_blurTexCoords[ 4] = v_texcoord + vec2(-0.012 * strength, 0.0);
    v_blurTexCoords[ 5] = v_texcoord + vec2(-0.008 * strength, 0.0);
    v_blurTexCoords[ 6] = v_texcoord + vec2(-0.004 * strength, 0.0);
    v_blurTexCoords[ 7] = v_texcoord + vec2( 0.004 * strength, 0.0);
    v_blurTexCoords[ 8] = v_texcoord + vec2( 0.008 * strength, 0.0);
    v_blurTexCoords[ 9] = v_texcoord + vec2( 0.012 * strength, 0.0);
    v_blurTexCoords[10] = v_texcoord + vec2( 0.016 * strength, 0.0);
    v_blurTexCoords[11] = v_texcoord + vec2( 0.020 * strength, 0.0);
    v_blurTexCoords[12] = v_texcoord + vec2( 0.024 * strength, 0.0);
    v_blurTexCoords[13] = v_texcoord + vec2( 0.028 * strength, 0.0);
}
