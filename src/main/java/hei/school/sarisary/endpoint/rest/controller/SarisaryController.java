package hei.school.sarisary.endpoint.rest.controller;

import hei.school.sarisary.PojaGenerated;
import hei.school.sarisary.file.BucketComponent;
import lombok.AllArgsConstructor;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.io.File.createTempFile;

@RestController
@CrossOrigin
@PojaGenerated
public class SarisaryController {
    BucketComponent bucketComponent;
    public SarisaryController(BucketComponent bucketComponent) {
        this.bucketComponent = bucketComponent;
    }

    private String original = "original";
    private String transformed = "transformed";

    @PutMapping("/black-and-white/{id}")
    public ResponseEntity<String> image_can_be_uploaded_then_signed(
            @RequestBody File file, @PathVariable String id) throws IOException {
        MBFImage image = ImageUtilities.readMBF(file);
        var originalPrefix = id + original;
        var transformedPrefix = id + transformed;
        var suffix = ".png";

        var originalBucketKey = originalPrefix + suffix;
        var transformedBucketKey = transformedPrefix + suffix;

        can_upload_image_then_download_image(file, originalBucketKey);

        var imageEdited = image.getBand(0);
        var imageFileToUpload = createTempFile(transformedPrefix, suffix);
        writeImageContent(imageEdited, imageFileToUpload);

        can_upload_image_then_download_image(imageFileToUpload, transformedBucketKey);

        List<String> url = new ArrayList<>();
        url.add(can_presign(originalBucketKey).toString());
        url.add(can_presign(transformedBucketKey).toString());

        return ResponseEntity.of(Optional.of(url.toString()));
    }

    @GetMapping("/black-and-white/{id}")
    public ResponseEntity<Object> image_can_be_got(@PathVariable String id) {
        var originalPrefix = id + original;
        var transformedPrefix = id + transformed;
        var suffix = ".png";

        var originalBucketKey = originalPrefix + suffix;
        var transformedBucketKey = transformedPrefix + suffix;


        List<String> url = new ArrayList<>();
        url.add(can_presign(originalBucketKey).toString());
        url.add(can_presign(transformedBucketKey).toString());

        return ResponseEntity.of(Optional.of(url.toString()));
    }

    private void writeImageContent(FImage imageEdited, File imageFile) throws IOException {
        ImageUtilities.write(imageEdited, imageFile);
    }

    private File can_upload_image_then_download_image(File imageFileToUpload, String bucketKey)
            throws IOException {
        bucketComponent.upload(imageFileToUpload, bucketKey);
        var imageFileDownloaded = bucketComponent.download(bucketKey);
        var downloadedContent = Files.readString(imageFileDownloaded.toPath());
        var uploadedContent = Files.readString(imageFileToUpload.toPath());
        if (!uploadedContent.equals(downloadedContent)) {
            throw new RuntimeException("Uploaded and downloaded contents mismatch");
        }

        return imageFileDownloaded;
    }

    private Object can_presign(String imageFileBucketKey) {
        return bucketComponent.presign(imageFileBucketKey, Duration.ofMinutes(2));
    }
}
