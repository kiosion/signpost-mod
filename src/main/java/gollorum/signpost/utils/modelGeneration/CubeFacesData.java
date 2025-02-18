package gollorum.signpost.utils.modelGeneration;

import gollorum.signpost.utils.Tuple;
import net.minecraft.core.Direction;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CubeFacesData<TextureIdentifier> {
    public final Direction direction;
    public final TextureIdentifier texture;
    public final FaceRotation rotation;
    public final int textureSize;

    private CubeFacesData(Direction direction, TextureIdentifier texture, FaceRotation rotation, int textureSize) {
        this.direction = direction;
        this.texture = texture;
        this.rotation = rotation;
        this.textureSize = textureSize;
    }

    public static <TextureIdentifier> List<CubeFacesData<TextureIdentifier>> from(Function<Direction, Optional<Tuple<TextureIdentifier, Tuple<FaceRotation, Integer>>>> faceDataGetter) {
        return Arrays.stream(Direction.values())
            .map(d -> faceDataGetter.apply(d).map(fd -> Tuple.of(d, fd)))
            .filter(Optional::isPresent).map(Optional::get)
            .map(Tuple -> new CubeFacesData<>(Tuple._1, Tuple._2._1, Tuple._2._2._1, Tuple._2._2._2))
            .collect(Collectors.toList());
    }

    public static <TextureIdentifier> List<CubeFacesData<TextureIdentifier>> uniform(TextureIdentifier texture, FaceRotation rotation, int textureSize, Direction... directions) {
        return Arrays.stream(directions).map(d -> new CubeFacesData<>(d, texture, rotation, textureSize)).collect(Collectors.toList());
    }

    public static <TextureIdentifier> List<CubeFacesData<TextureIdentifier>> all(TextureIdentifier texture, FaceRotation rotation, int textureSize, Predicate<Direction> where) {
        return Arrays.stream(Direction.values())
            .filter(where)
            .map(d -> new CubeFacesData<>(d, texture, rotation, textureSize))
            .collect(Collectors.toList());
    }

}
