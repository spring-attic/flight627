var startsWith = require('./strings').startsWith;

/**
 * This function is similar to nodejs path.resolve with a few notable differences.
 *   1) always uses '/' as path separators.
 *   2) treats relative base paths differently. They are kept relative rather
 *      than resolved based on process.env.pwd
 */
function pathResolve(basePath, resolvePath) {
	if (!basePath) {
		return pathNormalize(resolvePath);
	}
	if (typeof(resolvePath) === 'string') {
		if (basePath === '.' || resolvePath[0] === '/') {
			return pathNormalize(resolvePath);
		} else {
			return pathNormalize(basePath + '/' + resolvePath);
		}
	}
}

function pathNormalize(path) {
	path = path.replace(/\\/g, '/');
	var segments = path.split('/');
	var normalized = [];
	var i = 0;
	while (i < segments.length) {
		var segment = segments[i];
		if (segment==='.') {
			//skip
		} else if (segment==='' && i>0) {
			//This means there were consecutive slashes or a trailing slash: ignore!
		} else if (segment==='..') {
			if (normalized.length>0) {
				var prevSeg = normalized[normalized.length-1];
				if (prevSeg==='..') {
					normalized.push(segment);
				} else {
					normalized.splice(-1, 1);
				}
			} else {
				normalized.push(segment);
			}
		} else {
			normalized.push(segment);
		}
		i++;
	}
	return normalized.join('/') || (path[0]==='/' ? '/' : '.');
}

function getFileName(handle) {
	var segments = handle.split('/');
	return segments[segments.length-1];
}

function getDirectory(handle) {
	if (handle.length===3 && handle.substring(1)===':/') {
		//Special case for windows path like "C:/"
		//We should return null for the parent.
		return null;
	}
	if (handle==='.' || handle === '/') {
		return null;
	}
	var segments = handle.split('/');
	if (segments.length===1) {
		return '.';
	} else {
		segments.splice(-1, 1);
		var result = segments.join('/');
		if (result.length===2 && result[1]===':') {
			//Special case for windows: we get this 'C:' as parent of 'C:/something'
			//What we want instead is 'C:/'
			return result+'/';
		} else {
			return result || '/';
		}
	}
}

/**
 * Checks whether one path is a prefix of another path being tolerant of
 * an optional trailing slash at the end of the prefix path.
 */
function pathIsPrefixOf(prefix, path) {
	//Remove any trailing slashes from prefix
	while (prefix[prefix.length-1]==='/') {
		prefix = prefix.substring(0, prefix.length-1);
	}
	if (startsWith(path, prefix)) {
		//It is prefix... stringwise, so probably a path prefix...
		// But... the caveat still to check is that
		// maybe our prefix stops in the midle of a path segment.
		return path.length===prefix.length || path[prefix.length]==='/';
	}
	return false;
}

/**
 * Join two path strings together being careful not to
 * create double slashes in the process.
 * <p>
 * In most cases use of this function could be replaced by
 * pathResolve, but this is more efficient since it
 * doesn't normalize the resulting path.
 */
function pathJoin(p1, p2) {
	var sep = p1[p1.length-1]==='/'?'' : '/';
	if (p2[0] === '/') {
		p2 = p2.substring(1);
	}
	return p1 + sep + p2;
}

exports.pathJoin = pathJoin;
exports.pathNormalize = pathNormalize;
exports.pathResolve = pathResolve;
exports.getFileName = getFileName;
exports.getDirectory = getDirectory;
exports.pathIsPrefixOf = pathIsPrefixOf;