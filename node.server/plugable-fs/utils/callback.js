//eachk :: ([a], (a, Continuation<Void>) -> Void, Coninuation<Void>) -> Void
// This is a 'foreach' on an array, where the function that needs to be called on the
// elements of the array is callback style. I.e. the function calls some other function when its
// work is done. Since this is a 'each' rather than a 'map', we don't care about the 'return values'
// of the functions (and in callback style, this means, the parameters of the callbacks).
function eachk(array, f, callback) {
	if (!callback) {
		throw new Error("No callback provided");
	}
	function loop(i) {
		if (i < array.length) {
			f(array[i], function() {
				loop(i + 1);
			});
		} else {
			callback();
		}
	}
	loop(0);
}

//Map a function onto an array in callback (continuation passing) style:
//Actually this isn't really a 'CPS style'. It's a parrallel version of map.
function mapk(array, f, k) {
	if (array.length===0) {
		//Special case for zero length, otherwise k won't get called.
		return k([]);
	}
	var remaining = array.length;
	var newArray = [];
	function makeCallback(i) {
		return function (r) {
			newArray[i] = r;
			remaining--;
			if (remaining===0) {
				//All results received!
				k(newArray);
			}
		};
	}
	for (var i=0; i<array.length; i++) {
		f(array[i], makeCallback(i));
	}

}

exports.mapk = mapk;
exports.eachk = eachk;