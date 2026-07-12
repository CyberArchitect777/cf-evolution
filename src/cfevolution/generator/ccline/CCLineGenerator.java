/*
 * CF Evolution: An editor for Formula One Grand Prix/World Circuit
 * Copyright (C) 2005-2007  The Chequered Flag Development Team
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package cfevolution.generator.ccline;

/**
    One automatic best line generation method. Implementations live in the
    approach subpackages (geometric, datafit, refine) and are run on a
    background thread by the generation dialog.
*/
public interface CCLineGenerator {

    /** Short human-readable method name for the dialog title. */
    String getName();

    /** Generates a candidate CCLine. Returns null only if cancelled.
        Throws Exception with a user-presentable message on failure
        (e.g. no training data found). */
    CCLineGenerationResult generate(CCLineGeneratorContext context,
                                    CCLineProgressListener listener) throws Exception;
}
